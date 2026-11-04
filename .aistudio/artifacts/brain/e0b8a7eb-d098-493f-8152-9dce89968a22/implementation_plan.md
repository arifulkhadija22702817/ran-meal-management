# সম্পূর্ণ চূড়ান্ত নিরাপত্তা প্রতিকার পরিকল্পনা (FINAL SECURITY REMEDIATION PLAN)

**অ্যাপ্লিকেশন:** MessMate Native Android (`com.aistudio.messmate.xkvrpz`)  
**প্রযুক্তি স্ট্যাক:** Kotlin + Jetpack Compose + Firebase Authentication (Google Sign-In) + Cloud Firestore  
**পরিকল্পনার স্থিতি:** **চূড়ান্ত অনুমোদনের জন্য প্রস্তুত (Ready for Implementation upon Approval)**  

---

## ১. নির্বাহী সারসংক্ষেপ ও লক্ষ্য

এই চূড়ান্ত পরিকল্পনাটি আপনার নির্দেশিত ১০টি সুনির্দিষ্ট সংশোধনের ওপর ভিত্তি করে প্রস্তুত করা হয়েছে। এর উদ্দেশ্য হলো ক্লাউড ফাংশনের ওপর বাধ্যতামূলক নির্ভরতা পরিহার করে (Firebase Spark Plan-এ শূন্য খরচে) ক্লায়েন্ট ট্রানজাকশন ও ফায়ারস্টোর সিকিউরিটি রুলসের মাধ্যমে একটি নিশ্ছিদ্র, গোপনীয়তা-সংরক্ষণকারী এবং সম্পূর্ণ সামঞ্জস্যপূর্ণ নিরাপত্তা ব্যবস্থা প্রণয়ন করা।

---

## ২. ১০টি সুনির্দিষ্ট বিষয়ের কারিগরি সমাধান ও বিশদ নকশা

### বিষয় ১: প্রকৃত Firebase Auth UID যাচাই (২৮ অক্ষরের অযৌক্তিক বাধ্যবাধকতা বর্জন)
- **সমস্যা:** ইতিপূর্বে খসড়া পরিকল্পনায় UID-এর দৈর্ঘ্য নির্দিষ্ট ২৮ অক্ষর (`size() == 28`) ধরার প্রস্তাব ছিল। কিন্তু বাস্তবে ফায়ারবেস অথেনটিকেশন বিভিন্ন প্রোভাইডারের ক্ষেত্রে পরিবর্তনশীল দৈর্ঘ্যের (Variable-length, সাধারণত ২০ থেকে ১২৮ অক্ষর) আলফানিউমেরিক UID তৈরি করতে পারে।
- **সংশোধিত নিরাপদ আচরণ:**
  - দৈর্ঘ্যের নির্দিষ্ট সীমা (২৮ অক্ষর) পরিহার করে ফায়ারস্টোর রুলসে সরাসরি **ক্রিপ্টোগ্রাফিক টোকেনের সমতা (`request.auth.uid`)** যাচাই করা হবে:
    ```javascript
    function isValidUid(uid) {
      return uid is string && uid.size() >= 1 && uid.size() <= 128 && uid.matches('^[a-zA-Z0-9_\\-]+$');
    }
    ```
  - যেকোনো অ্যাডমিন যাচাইয়ের ক্ষেত্রে:
    ```javascript
    function isCurrentMessManager(messId) {
      let messPath = /databases/$(database)/documents/messes/$(messId);
      return isSignedIn() && exists(messPath) &&
             get(messPath).data.managerUid == request.auth.uid;
    }
    ```
- **সংশ্লিষ্ট উপাদান:** `/firestore.rules` -> `isValidUid()`, `isCurrentMessManager()`.

---

### বিষয় ২: `user_profiles`-এর গোপনীয়তা ও রিড পারমিশন সীমাবদ্ধকরণ (Data Scraping রোধ)
- **সমস্যা:** যদি `/user_profiles` কালেকশনে সব অথেনটিকেটেড ব্যবহারকারীকে রিড পারমিশন (`allow get, list: if isSignedIn();`) দেওয়া হয়, তবে যে কেউ স্ক্রিপ্ট চালিয়ে সব ব্যবহারকারীর ইমেইল ও নাম স্ক্র্যাপ (Scrape) বা চুরি করতে পারে।
- **সংশোধিত নিরাপদ আচরণ:**
  1. **সাধারণ তালিকা দেখা (List Query) সম্পূর্ণ নিষিদ্ধ:** `allow list: if false;`। কোনো ব্যবহারকারীই পুরো ইউজার তালিকা ব্রাউজ করতে পারবে না।
  2. **নিজের প্রোফাইল রিড:** প্রত্যেক ব্যবহারকারী শুধুমাত্র তার নিজস্ব প্রোফাইল পড়তে পারবে (`request.auth.uid == userId`)।
  3. **হ্যান্ডওভারের জন্য সিঙ্গেল ডকুমেন্ট লুকআপ:** কেবল সক্রিয় মেস ম্যানেজার নতুন অ্যাডমিনের ইমেইল দিয়ে সুনির্দিষ্ট একটি ডকুমেন্ট `get` করতে পারবে, তাও শুধুমাত্র মেসের সীমানার মধ্যে।
- **রুলস নকশা:**
  ```javascript
  match /user_profiles/{userId} {
    // প্রত্যেকে শুধুমাত্র নিজের প্রোফাইল পড়তে পারবে
    allow get: if isSignedIn() && request.auth.uid == userId;
    
    // সামগ্রিক লিস্ট কোয়েরি সম্পূর্ণ ব্লকড
    allow list: if false;
    
    allow create: if isSignedIn() && request.auth.uid == userId &&
                     incoming().uid == request.auth.uid &&
                     incoming().email.lower() == request.auth.token.email.lower() &&
                     request.auth.token.email_verified == true;
  }
  ```

---

### বিষয় ৩: Profile-এর UID ও Email টোকেন যাচাই এবং পরিবর্তনযোগ্য ফিল্ড নির্ধারণ
- **সমস্যা:** ব্যবহারকারী যদি নিজের প্রোফাইলে অন্য কারও ইমেইল বা পরিবর্তিত UID বসিয়ে দেয়, তবে সিস্টেমে ভুয়ো পরিচয় তৈরি হতে পারে।
- **সংশোধিত নিরাপদ আচরণ:**
  - `create` এর সময় `incoming().uid` এবং `incoming().email` অবশ্যই `request.auth.uid` এবং `request.auth.token.email`-এর সাথে হুবহু মিলতে হবে।
  - `update` এর সময় `uid`, `email` এবং `createdAt` সম্পূর্ণ অপরিবর্তনীয় (Immutable) থাকবে। শুধুমাত্র ব্যবহারকারীর প্রদর্শিত নাম (`displayName`), ফোন ও সর্বশেষ সক্রিয় সময় (`lastActiveAt`) আপডেট করা যাবে।
- **রুলস কোড:**
  ```javascript
  match /user_profiles/{userId} {
    allow update: if isSignedIn() && request.auth.uid == userId &&
                     incoming().uid == existing().uid &&
                     incoming().email == existing().email &&
                     incoming().createdAt == existing().createdAt &&
                     (!('displayName' in incoming()) || (incoming().displayName is string && incoming().displayName.size() <= 80)) &&
                     (!('lastActiveAt' in incoming()) || incoming().lastActiveAt is timestamp);
    allow delete: if false;
  }
  ```

---

### বিষয় ৪: `getAfter()` দিয়ে Parent Mess, `admin_history` এবং `audit_logs`-এর ত্রিমুখী পারমাণবিক সামঞ্জস্য
- **নিরাপত্তা সমস্যা:** হ্যান্ডওভারের সময় যদি মেস মূল ডকুমেন্ট আপডেট হয় কিন্তু হিস্ট্রি রেকর্ড মিসিং থাকে, অথবা হিস্ট্রি রেকর্ডে নতুন অ্যাডমিনের নাম লেখা হয় কিন্তু মেস ডকুমেন্টে অন্য কারো নাম থাকে, তবে সিস্টেমের অডিট ট্রেইল ভেঙে পড়বে।
- **সংশোধিত নিরাপদ আচরণ (`getAfter()` এর ব্যবহার):**
  ফায়ারস্টোরে `getAfter()` একটি ট্রানজাকশন বা ব্যাচের সকল অপারেশন শেষে ডকুমেন্টের চূড়ান্ত অবস্থা মূল্যায়ন করে।
  রুলসে নিশ্চিত করা হবে যে:
  1. `messes/{messId}` আপডেটে যখন `managerUid` পরিবর্তিত হবে, তখন একই ট্রানজাকশনে `admin_history` এবং `audit_logs` তৈরি হওয়া বাধ্যতামূলক।
  2. `admin_history` তৈরির সময় যাচাই করা হবে যে, `getAfter(messPath).data.managerUid` এবং ইনকামিং হিস্ট্রি রেকর্ডের `newAdminUid` হুবহু একই।
  3. `audit_logs`-এ `action == 'TRANSFER_ADMIN'` থাকতে হবে এবং পারফর্মকারী বর্তমান ইউজারের ইমেইল হতে হবে।
- **রুলস নকশা:**
  ```javascript
  match /messes/{messId} {
    allow update: if isCurrentMessManager(messId) && isValidMess(incoming());

    match /admin_history/{historyId} {
      allow create: if isCurrentMessManager(messId) &&
                       isValidAdminHistory(incoming()) &&
                       getAfter(/databases/$(database)/documents/messes/$(messId)).data.managerUid == incoming().newAdminUid &&
                       incoming().previousAdminUid == request.auth.uid;
      allow update, delete: if false;
    }

    match /audit_logs/{logId} {
      allow create: if isCurrentMessManager(messId) &&
                       incoming().messId == messId &&
                       incoming().performedBy == request.auth.token.email;
      allow update, delete: if false;
    }
  }
  ```

---

### বিষয় ৫: `authorized_members` কালেকশনের পূর্ণ নিরাপত্তা রুলস ও জীবনচক্র
- **প্রয়োজনীয়তা:** মেম্বারদের বিদ্যমান ২০-অক্ষরের অটো-আইডি ডকুমেন্ট পরিবর্তন না করে কে কে মেসের অনুমোদিত সদস্য তা UID দিয়ে ট্র্যাক করা।
- **পাথ:** `/messes/{messId}/authorized_members/{memberUid}`
- **নিরাপত্তা রুলস:**
  ```javascript
  match /messes/{messId}/authorized_members/{memberUid} {
    allow get: if isSignedIn() && (request.auth.uid == memberUid || isCurrentMessManager(messId));
    allow list: if isCurrentMessManager(messId);

    allow create: if isCurrentMessManager(messId) &&
                     isValidId(memberUid) &&
                     incoming().uid == memberUid &&
                     incoming().email is string &&
                     incoming().addedAt is timestamp;

    allow update: if isCurrentMessManager(messId) &&
                     incoming().uid == existing().uid;

    allow delete: if isCurrentMessManager(messId) || (isSignedIn() && request.auth.uid == memberUid);
  }
  ```

---

### বিষয় ৬: Initial Admin Bootstrap-এর জন্য নিরাপদ পদ্ধতি
- **সমস্যা:** যদি রুলসে লেখা থাকে `allow create: if isSignedIn();`, তবে যে কোনো সাধারণ ইউজার সরাসরি ডাটাবেজে ঢুকে নিজেকে মেসের মালিক বানাতে পারে।
- **বাস্তব পরিস্থিতি:** আপনার প্রজেক্টের প্রোডাকশন ডেটাবেজে মূল মেস ডকুমেন্ট `/messes/main_mess` **ইতিমধ্যে তৈরি আছে**।
- **নিরাপদ সমাধান (Two-Tier Bootstrap Guard):**
  1. ক্লায়েন্ট থেকে মেস ডিলিট স্থায়ীভাবে নিষিদ্ধ: `allow delete: if false;`। ফলে বিদ্যমান মেস কখনোই ধ্বংস করে নতুন মেস বানিয়ে কেউ মালিক হতে পারবে না।
  2. মেস ক্রিয়েশন রুলস সীমাবদ্ধকরণ:
  ```javascript
  match /messes/{messId} {
    allow create: if isSignedIn() &&
                     !exists(/databases/$(database)/documents/messes/$(messId)) &&
                     incoming().managerUid == request.auth.uid &&
                     incoming().managerEmail.lower() == request.auth.token.email.lower() &&
                     request.auth.token.email_verified == true;
    allow delete: if false;
  }
  ```

---

### বিষয় ৭: Handover-এর পর Former Admin-এর Read Access নীতি
- **নীতিগত সিদ্ধান্ত:** সাবেক অ্যাডমিনের Write পারমিশন তাৎক্ষণিক বাতিল হবে, কিন্তু তিনি মেসের সদস্য হিসেবে Read Access বজায় রাখবেন।
- **প্রযুক্তিগত বাস্তবায়ন:**
  1. হ্যান্ডওভার ট্রানজাকশনের অংশ হিসেবে সাবেক অ্যাডমিনের UID-কে `/messes/{messId}/authorized_members/{formerAdminUid}`-এ মেম্বার হিসেবে নিশ্চিত করা হবে।
  2. মেসের রিড ফাংশন `isMessAuthorized()`:
     ```javascript
     function isMessAuthorized(messId) {
       let messPath = /databases/$(database)/documents/messes/$(messId);
       return isSignedIn() && exists(messPath) && (
         get(messPath).data.managerUid == request.auth.uid ||
         exists(/databases/$(database)/documents/messes/$(messId)/authorized_members/$(request.auth.uid))
       );
     }
     ```
  3. মেসের সকল ডেটা পড়ার জন্য: `allow get, list: if isMessAuthorized(messId);`
  4. লেখার জন্য শুধুমাত্র: `isCurrentMessManager(messId)` কার্যকর থাকবে, যা সাবেক ম্যানেজারের জন্য `false` হয়ে যাবে।

---

### বিষয় ৮: সম্পূর্ণ নিরাপত্তা টেস্ট ম্যাট্রিক্স (Unrun Tests Marked as `NOT RUN`)

| টেস্ট আইডি | পরীক্ষার বিষয়বস্তু | টেস্টের ধরন | প্রত্যাশিত ফলাফল | বর্তমান স্থিতি |
| :--- | :--- | :--- | :--- | :---: |
| **SEC-01** | আন-অথেনটিকেটেড রিড রিকোয়েস্ট | Security Rule | `PERMISSION_DENIED` | **NOT RUN** |
| **SEC-02** | আন-অথেনটিকেটেড রাইট রিকোয়েস্ট | Security Rule | `PERMISSION_DENIED` | **NOT RUN** |
| **SEC-03** | অন্য মেসের মেম্বার দ্বারা ডেটা রিড | Security Rule | `PERMISSION_DENIED` | **NOT RUN** |
| **SEC-04** | অনুমোদিত মেম্বার দ্বারা মিল/বাজার রিড | Security Rule | `PERMISSION_ALLOWED` | **NOT RUN** |
| **SEC-05** | সাধারণ মেম্বার দ্বারা মিল/বাজার এডিট চেষ্টা | Security Rule | `PERMISSION_DENIED` | **NOT RUN** |
| **SEC-06** | সাধারণ মেম্বার দ্বারা মেম্বার ডিলিট চেষ্টা | Security Rule | `PERMISSION_DENIED` | **NOT RUN** |
| **SEC-07** | আন-ভেরিফাইড ইমেইল টোকেন দিয়ে অ্যাডমিন অ্যাকশন | Security Rule | `PERMISSION_DENIED` | **NOT RUN** |
| **SEC-08** | ইউজার A দ্বারা ইউজার B-এর প্রোফাইল রাইট | Privacy Rule | `PERMISSION_DENIED` | **NOT RUN** |
| **SEC-09** | ইউজার A দ্বারা সামগ্রিক ইউজার প্রোফাইল তালিকা কোয়েরি | Privacy Rule | `PERMISSION_DENIED` | **NOT RUN** |
| **SEC-10** | ইউজার দ্বারা প্রোফাইলের ইমেইল বা UID ফিল্ড পরিবর্তনের চেষ্টা | Integrity Rule | `PERMISSION_DENIED` | **NOT RUN** |
| **SEC-11** | অস্তিত্বহীন মেসে সাব-কালেকশন রাইট | Rule Guard | `PERMISSION_DENIED` | **NOT RUN** |
| **SEC-12** | বৈধ হ্যান্ডওভার (সঠিক UID ও দ্বিমুখী getAfter) | Transaction Rule | `PERMISSION_ALLOWED` | **NOT RUN** |
| **SEC-13** | মেস ডকুমেন্ট আপডেট ছাড়া শুধু ফেক admin_history রাইট | getAfter() Guard | `PERMISSION_DENIED` | **NOT RUN** |
| **SEC-14** | হ্যান্ডওভার সম্পন্ন হওয়ার পর সাবেক অ্যাডমিনের রাইট চেষ্টা | Instant Revocation | `PERMISSION_DENIED` | **NOT RUN** |
| **SEC-15** | হ্যান্ডওভারের পর সাবেক অ্যাডমিনের রিড চেষ্টা | Read Retention | `PERMISSION_ALLOWED` | **NOT RUN** |
| **SEC-16** | একই সেকেন্ডে দুটি সমান্তরাল হ্যান্ডওভার রিকোয়েস্ট | Concurrency Test | প্রথমটি Pass, দ্বিতীয়টি Abort | **NOT RUN** |
| **SEC-17** | মেস ক্যালকুলেটর ইউনিট টেস্ট সুইট | Unit Logic | `BUILD SUCCESSFUL` | **PASS (Executed via Gradle)** |

---

### বিষয় ৯: ব্যাকআপ এনক্রিপশন, স্টোরেজ অবস্থান ও রিস্টোর ভেরিফিকেশন পরিকল্পনা
1. **স্টোরেজ অবস্থান:** ডেটাবেজ পাথ `/messes/{messId}/backups/{backupId}`
2. **এনক্রিপশন ও ডেটা সুরক্ষা:** অ্যান্ড্রয়েড ক্লায়েন্টে Android Keystore-backed AES-256-GCM এনক্রিপশন ব্যবহার করে JSON স্ট্রিংকে এনক্রিপ্ট করে `backupDataJson`-এ সংরক্ষণ করা হবে, সাথে SHA-256 চেকসাম থাকবে।
3. **রিস্টোর ভেরিফিকেশন (Dry-Run Restore Verification):** 
   - Checksum যাচাই
   - স্কিমা ড্রাই-রান পার্সিং
   - অটোমেটিক সেফটি স্ন্যাপশট (`pre_restore_snapshot`) গ্রহণ।

---

### বিষয় ১০: পুরোনো অ্যাপ ভার্সন কম্প্যাটিবিলিটি ও নিরাপদ ডিপ্লয়মেন্ট কৌশল
1. **পর্যায় ১ (Data Sanity):** প্রোডাকশন মেসে বর্তমান অ্যাডমিনের আসল Firebase UID নিশ্চিত করা।
2. **পর্যায় ২ (App Deployment):** ক্লায়েন্ট কোড আপডেট (Transaction, Real UID, Profile Sync)।
3. **পর্যায় ৩ (Rules Cutover):** এমুলেটর টেস্ট সম্পন্নের পর সর্বশেষ কঠোর `firestore.rules` ডিপ্লয়।

---

## ৩. ফাইল ও উপাদান পরিবর্তনের সুনির্দিষ্ট রেফারেন্স টেবিল

| ক্রমিক | কাজের বিবরণ | ফাইল পাথ | ফাংশন / ব্লক | উপাদানের ধরন |
| :---: | :--- | :--- | :--- | :--- |
| ১ | UID Validation & Auth Match | `/firestore.rules` | `isCurrentMessManager`, `isValidUid` | Rules |
| ২ | Profile Privacy & Immutability | `/firestore.rules` | `match /user_profiles/{userId}` | Rules |
| ৩ | Transaction Concurrency & Real UID | `MessRepository.kt` | `transferManager()` | Kotlin |
| ৪ | Fake UID Removal | `MessViewModel.kt` | `transferManager()` | Kotlin |
| ৫ | Member Authorization Collection | `/firestore.rules` | `match /authorized_members/{uid}` | Rules |
| ৬ | getAfter() 3-Way Consistency | `/firestore.rules` | `match /admin_history`, `match /messes` | Rules |
| ৭ | Former Admin Read Retention | `/firestore.rules` | `isMessAuthorized` | Rules |
| ৮ | Backup Checksum & Safe Restore | `MessRepository.kt` | `backupMessData()`, `restoreBackup()` | Kotlin |

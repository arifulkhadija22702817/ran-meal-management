# সংশোধিত চূড়ান্ত নিরাপত্তা প্রতিকার পরিকল্পনা (Revised Final Security Remediation Plan)

**তারিখ:** নভেম্বর ২০২৬  
**টার্গেট প্ল্যাটফর্ম:** Android (Kotlin, Jetpack Compose)  
**ব্যাকএন্ড:** Firebase Firestore & Firebase Authentication (Google Sign-In)  
**পরিকাঠামো সীমা:** Firebase Spark Plan (বিনামূল্যে ব্যবহারের উপযুক্ত; কোনো ক্লাউড ফাংশন বা পেইড এক্সটার্নাল এপিআই প্রয়োজন নেই)  
**মূল নীতি:** প্রকৃত Firebase Auth UID-ই পরিচয়ের একমাত্র ভিত্তি। Email কখনো অথোরাইজেশনের একক নির্ধারক হিসেবে ব্যবহৃত হবে না।

---

## ১. নতুন Admin-এর প্রকৃত Firebase UID সংগ্রহের নিরাপদ Invitation/UID Exchange System (Spark Plan Friendly)

### সমস্যা:
ম্যানেজার যখন অন্য কাউকে দায়িত্ব হস্তান্তর করতে চান, তখন তিনি কেবল নতুন ম্যানেজারের ইমেইল বা ফোন নম্বর জানেন। নতুন ম্যানেজারের প্রকৃত ২৮-অক্ষরের ক্রিপ্টোগ্রাফিক Firebase UID সরাসরি টাইপ বা কপি-পেস্ট করা ব্যবহারকারীর জন্য অত্যন্ত জটিল এবং ভুল হওয়ার ঝুঁকি থাকে। তাছাড়া ক্লাউড ফাংশন ছাড়া কীভাবে এটি নিরাপদে এক্সচেঞ্জ করা সম্ভব?

### সমাধান ও আর্কিটেকচার:
ফায়ারবেস স্পার্ক প্ল্যানে ক্লায়েন্ট-টু-ফায়ারস্টোর টু-ওয়ে হ্যান্ডশেক মডেল:

```
[বর্তমান ম্যানেজার]                    [ফায়ারস্টোর সাব-কালেকশন]               [প্রার্থী/নতুন ম্যানেজার]
      |                                       |                                       |
      |-- ১. ইনভাইটেশন তৈরি (ইমেইল দিয়ে) --->|                                       |
      |   /invitations/{inviteId}             |                                       |
      |   (status: 'pending')                 |                                       |
      |                                       |<-- ২. গুগল সাইন-ইন করে ইনভাইটেশন খোঁজা --|
      |                                       |-- ৩. ইনভাইটেশন গ্রহণ ও UID বাইন্ডিং ---->|
      |                                       |   (status: 'claimed', claimedUid: auth.uid)
      |<-- ৪. ড্রপডাউনে ভেরিফাইড ইউজার দর্শন --|
      |-- ৫. পারমাণবিক হ্যান্ডওভার সম্পাদন ---->| (আসল UID দিয়ে ট্রানজাকশন সম্পন্ন)
```

### ফায়ারস্টোর ডেটা মডেল (`/messes/{messId}/invitations/{inviteId}`):
```json
{
  "inviteId": "inv_202611_01",
  "targetEmail": "newmanager@gmail.com",
  "role": "manager_candidate",
  "status": "pending", // pending -> claimed -> completed -> revoked
  "claimedUid": null,
  "claimedEmail": null,
  "createdByUid": "current_manager_uid",
  "createdAt": "2026-11-03T10:00:00Z",
  "expiresAt": "2026-11-05T10:00:00Z"
}
```

### ফায়ারস্টোর সিকিউরিটি রুলস লজিক:
```javascript
match /messes/{messId}/invitations/{inviteId} {
  // ১. শুধুমাত্র বর্তমান সক্রিয় মেস ম্যানেজার ইনভাইটেশন তৈরি ও বাতিল করতে পারবেন
  allow create: if isCurrentMessManager(messId)
    && request.resource.data.keys().hasOnly(['inviteId', 'targetEmail', 'role', 'status', 'claimedUid', 'claimedEmail', 'createdByUid', 'createdAt', 'expiresAt'])
    && request.resource.data.createdByUid == request.auth.uid
    && request.resource.data.status == 'pending'
    && request.resource.data.claimedUid == null
    && request.resource.data.role == 'manager_candidate'
    && request.resource.data.targetEmail is string;

  // ২. রিড পারমিশন: বর্তমান ম্যানেজার অথবা যার ইমেইলে ইনভাইট পাঠানো হয়েছে তিনি পড়তে পারবেন
  allow read: if isCurrentMessManager(messId) 
    || (isSignedIn() && request.auth.token.email.lower() == resource.data.targetEmail.lower());

  // ৩. ইনভাইটেশন ক্লেইম (নতুন প্রার্থী):
  // প্রার্থী শুধুমাত্র তখনই ক্লেইম করতে পারবে যদি তার গুগল ভেরিফাইড ইমেইল targetEmail-এর সাথে হুবহু মিলে
  allow update: if isSignedIn()
    && request.auth.token.email_verified == true
    && (
      // কেস ক: প্রার্থী নিজে ক্লেইম করছে
      (resource.data.status == 'pending'
        && request.resource.data.status == 'claimed'
        && request.auth.token.email.lower() == resource.data.targetEmail.lower()
        && request.resource.data.claimedUid == request.auth.uid
        && request.resource.data.claimedEmail == request.auth.token.email.lower()
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status', 'claimedUid', 'claimedEmail', 'claimedAt']))
      ||
      // কেস খ: ম্যানেজার ইনভাইটেশন বাতিল বা সম্পূর্ণ করছেন
      (isCurrentMessManager(messId)
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status']))
    );

  allow delete: if isCurrentMessManager(messId);
}
```

### যেভাবে কাজ করবে:
1. বর্তমান ম্যানেজার অ্যাপের অ্যাডমিন প্যানেলে নতুন ম্যানেজারের গুগল ইমেইল ইনপুট দিয়ে "Send Invite" চাপবেন।
2. নতুন ব্যক্তি অ্যাপটি ইনস্টল করে তার জিমেইল দিয়ে সাইন-ইন করবেন। অ্যাপ স্বয়ংক্রিয়ভাবে তার ইমেইলের জন্য কোনো পেন্ডিং ইনভাইটেশন আছে কিনা তা কুয়েরি করবে।
3. প্রার্থীর অ্যাপ স্ক্রিনে "আপনাকে মেস ম্যানেজারের দায়িত্ব গ্রহণের জন্য আমন্ত্রণ জানানো হয়েছে" মেসেজ দেখাবে। "Accept" চাপলে ফায়ারস্টোর রুলসের অধীনে তার আসল `request.auth.uid` ইনভাইটেশন ডকুমেন্টে এবং `authorized_members`-এ সেভ হবে।
4. বর্তমান ম্যানেজারের ফোনে সাথে সাথে প্রার্থীর নাম এবং ক্রিপ্টোগ্রাফিক UID সহ "Ready for Handover" ব্যাজ দৃশ্যমান হবে। ম্যানেজার একক ক্লিকে ট্রানজাকশন সম্পন্ন করবেন।

---

## ২. `user_profiles`-এর Create ও Update-এ কঠোর Field Allowlist এবং `affectedKeys().hasOnly()`

### সমস্যা:
যদি ফিল্ড সীমাবদ্ধ না থাকে, তবে কোনো অসাধু ব্যবহারকারী নিজের প্রোফাইলে `isAdmin: true`, `role: 'superadmin'`, `permissions: ['all']` ইত্যাদি বানিয়ে ইনজেক্ট করতে পারে এবং অনিয়ন্ত্রিত ফিল্ড জমিয়ে ডেটাবেজের ক্ষতি করতে পারে।

### ফায়ারস্টোর সিকিউরিটি রুলস লজিক:
```javascript
match /user_profiles/{userId} {
  // সমগ্র প্রোফাইল লিস্ট করা পুরোপুরি নিষিদ্ধ (ইউজার ডেটা স্ক্র্যাপিং রোধে)
  allow list: if false;

  // শুধু নিজের প্রোফাইল রিড করতে পারবে, অথবা মেস ম্যানেজার ভেরিফিকেশনের জন্য গেট করতে পারবেন
  allow get: if isSignedIn() && (request.auth.uid == userId || isCurrentMessManager('default_mess'));

  // ১. Create রুলস: কঠোর হোয়াইটলিস্ট
  allow create: if isSignedIn()
    && request.auth.uid == userId
    && request.auth.token.email_verified == true
    && request.resource.data.keys().hasOnly([
        'uid', 'email', 'displayName', 'photoUrl', 
        'phone', 'createdAt', 'updatedAt', 'fcmToken', 'appVersionCode'
       ])
    && request.resource.data.uid == request.auth.uid
    && request.resource.data.email == request.auth.token.email
    && request.resource.data.displayName is string
    && request.resource.data.displayName.size() <= 100
    && request.resource.data.createdAt == request.time
    && request.resource.data.updatedAt == request.time;

  // ২. Update রুলস: অপরিবর্তনীয় ফিল্ড লক এবং শুধুমাত্র অনুমোদিত ফিল্ড আপডেট
  allow update: if isSignedIn()
    && request.auth.uid == userId
    // অপরিবর্তনীয় ফিল্ড যাচাই: uid, email, createdAt কখনো বদলানো যাবে না
    && request.resource.data.uid == resource.data.uid
    && request.resource.data.email == resource.data.email
    && request.resource.data.createdAt == resource.data.createdAt
    // শুধুমাত্র নির্দিষ্ট প্রোফাইল ফিল্ড মডিফাই করার অনুমতি
    && request.resource.data.diff(resource.data).affectedKeys().hasOnly([
        'displayName', 'photoUrl', 'phone', 'updatedAt', 'fcmToken', 'appVersionCode'
       ])
    && request.resource.data.updatedAt == request.time
    && (request.resource.data.displayName is string && request.resource.data.displayName.size() <= 100);

  // প্রোফাইল মুছে ফেলা ক্লায়েন্ট থেকে সম্পূর্ণ নিষিদ্ধ
  allow delete: if false;
}
```

### রুলসের কার্যকারিতা ব্যাখ্যা:
- `affectedKeys().hasOnly(...)`: ব্যবহারকারী শুধু তার ডিসপ্লে নাম, ছবি, ফোন, টোকেন বা অ্যাপ ভার্সন আপডেট করতে পারবে। অন্য কোনো ফিল্ড (যেমন `role`, `isAdmin`) পাঠালে ফায়ারস্টোর পারমিশন এরর দিয়ে রিকোয়েস্ট রিজেক্ট করবে।
- `request.resource.data.uid == resource.data.uid`: ক্লায়েন্ট নিজের ইউজার আইডি বা ইমেইল টেম্পারিং করতে পারবে না।

---

## ৩. Parent Mess, `admin_history` এবং `audit_logs`-এর মধ্যে প্রকৃত Three-Way Atomic Validation (Shared `handoverId` ও `getAfter()`)

### সমস্যা:
হ্যান্ডওভারের সময় ক্লায়েন্ট যদি ট্রানজাকশনে চালাকি করে মেস ম্যানেজারের নাম পরিবর্তন করে কিন্তু হিস্ট্রি না লেখে, অথবা হিস্ট্রিতে ভুল UID লিখে অডিট লগ স্কিপ করে, তবে তথ্যের মিল থাকে না এবং কারচুপি সম্ভব হয়।

### সমাধান (ত্রিমুখী ইন্টার-ডকুমেন্ট ভ্যালিডেশন):
হ্যান্ডওভারের সময় একটি ইউনিক `handoverId` (যেমন `hov_<timestamp>_<newUid>`) তৈরি করা হয়। একক পারমাণবিক ট্রানজাকশনে তিনটি ডকুমেন্ট লেখা হয়:
1. `/messes/{messId}`: আপডেট করা হয় নতুন `managerUid` এবং `lastHandoverId = handoverId` দিয়ে।
2. `/messes/{messId}/admin_history/{handoverId}`: ক্রিয়েট করা হয় যেখানে ডকুমেন্ট আইডি নিজেই `handoverId`।
3. `/messes/{messId}/audit_logs/{handoverId}`: ক্রিয়েট করা হয় একই `handoverId` ডকুমেন্ট আইডিতে।

### ফায়ারস্টোর সিকিউরিটি রুলস লজিক:

```javascript
// /messes/{messId} এর ভেতরে Update রুল
allow update: if isSignedIn() && (
  // সাধারণ মেস সেটিংস আপডেট (বর্তমান ম্যানেজার দ্বারা)
  (isCurrentMessManager(messId) && !request.resource.data.diff(resource.data).affectedKeys().hasAny(['managerUid', 'managerEmail', 'lastHandoverId', 'lastHandoverTime']))
  ||
  // পারমাণবিক মাসিক ম্যানেজার হস্তান্তর (Atomic Manager Handover)
  (isCurrentMessManager(messId)
    && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['managerUid', 'managerEmail', 'lastHandoverId', 'lastHandoverTime', 'updatedAt'])
    && request.resource.data.managerUid != resource.data.managerUid
    && request.resource.data.lastHandoverId is string
    && request.resource.data.lastHandoverTime == request.time
    // getAfter() দিয়ে admin_history এবং audit_logs-এর অস্তিত্ব ও সমতা যাচাই:
    && getAfter(/databases/$(database)/documents/messes/$(messId)/admin_history/$(request.resource.data.lastHandoverId)).data.handoverId == request.resource.data.lastHandoverId
    && getAfter(/databases/$(database)/documents/messes/$(messId)/admin_history/$(request.resource.data.lastHandoverId)).data.newManagerUid == request.resource.data.managerUid
    && getAfter(/databases/$(database)/documents/messes/$(messId)/admin_history/$(request.resource.data.lastHandoverId)).data.previousManagerUid == resource.data.managerUid
    && getAfter(/databases/$(database)/documents/messes/$(messId)/audit_logs/$(request.resource.data.lastHandoverId)).data.handoverId == request.resource.data.lastHandoverId
    && getAfter(/databases/$(database)/documents/messes/$(messId)/audit_logs/$(request.resource.data.lastHandoverId)).data.action == 'MONTHLY_MANAGER_HANDOVER'
    && getAfter(/databases/$(database)/documents/messes/$(messId)/audit_logs/$(request.resource.data.lastHandoverId)).data.performedByUid == request.auth.uid)
);

// admin_history/{historyId} এর ভেতরে Create রুল
match /admin_history/{historyId} {
  allow create: if isSignedIn()
    && historyId == request.resource.data.handoverId
    && request.resource.data.keys().hasOnly(['handoverId', 'previousManagerUid', 'previousManagerEmail', 'newManagerUid', 'newManagerEmail', 'note', 'timestamp'])
    && request.resource.data.previousManagerUid == request.auth.uid
    && request.resource.data.timestamp == request.time
    // Parent Mess-এর getAfter() দিয়ে নিশ্চিত হওয়া যে সেখানে এই একই handoverId ও newManagerUid সেট হচ্ছে:
    && getAfter(/databases/$(database)/documents/messes/$(messId)).data.lastHandoverId == historyId
    && getAfter(/databases/$(database)/documents/messes/$(messId)).data.managerUid == request.resource.data.newManagerUid;
    
  allow read: if isMessAuthorized(messId);
  allow update, delete: if false; // ইতিহাস কখনো পরিবর্তন বা মোছা যাবে না
}

// audit_logs/{logId} এর ভেতরে Create রুল
match /audit_logs/{logId} {
  allow create: if isSignedIn() && (
    (logId == request.resource.data.handoverId
      && request.resource.data.action == 'MONTHLY_MANAGER_HANDOVER'
      && request.resource.data.performedByUid == request.auth.uid
      && request.resource.data.timestamp == request.time
      && getAfter(/databases/$(database)/documents/messes/$(messId)).data.lastHandoverId == logId)
    ||
    (isCurrentMessManager(messId) && request.resource.data.action != 'MONTHLY_MANAGER_HANDOVER')
  );
  
  allow read: if isMessAuthorized(messId);
  allow update, delete: if false;
}
```

### রুলসের কার্যকারিতা ব্যাখ্যা:
`getAfter()` ফাংশনটি ফায়ারস্টোরের সেই ট্রানজাকশনের চূড়ান্ত অবস্থাকে পরীক্ষা করে। যদি ট্রানজাকশনের ভেতর তিনটি ডকুমেন্টের যেকোনো একটিতে `handoverId` অমিল হয়, বা কোনো একটি বাদ দেওয়া হয়, তবে পুরো ট্রানজাকশনটি ফায়ারস্টোর ইঞ্জিন লেভেলে সাথে সাথে রোলব্যাক ও বাতিল হয়ে যাবে।

---

## ৪. `authorized_members`-এর কঠোর Update Restrictions এবং Former Admin-এর রিড পারমিশন রক্ষা

### সমস্যা:
1. মেম্বাররা কি নিজেদের রোল বা স্ট্যাটাস বদলে ফেলতে পারবে?
2. সাবেক ম্যানেজার যখন অন্য কাউকে দায়িত্ব হস্তান্তর করবেন, তখন ফায়ারস্টোর রুলসের কারণে কি তিনি মেসের হিসাবপত্র দেখার এক্সেস হারিয়ে ফেলবেন?

### সমাধান:
- মেম্বাররা নিজেদের রোল পরিবর্তন করতে পারবে না।
- সাবেক ম্যানেজারকে ট্রানজাকশনের সময় স্বয়ংক্রিয়ভাবে `authorized_members`-এ `role: 'member'` হিসেবে সংরক্ষণ করা হবে।
- রুলসে মেসের সব রিড অপারেশনে `isMessAuthorized(messId)` কল করা হয়, যা চেক করে:
  `request.auth.uid == mess.managerUid || exists(/databases/$(database)/documents/messes/$(messId)/authorized_members/$(request.auth.uid))`
- ফলে সাবেক ম্যানেজারের রাইট (Write) ক্ষমতা তৎক্ষণাৎ বাতিল হলেও মেম্বার হিসেবে তিনি অতীতের সব মিল, বাজার ও ব্যালেন্স অডিট করতে পারবেন।

### ফায়ারস্টোর সিকিউরিটি রুলস লজিক:
```javascript
match /authorized_members/{memberUid} {
  allow read: if isMessAuthorized(messId);

  // ১. Create রুল:
  allow create: if isSignedIn() && (
    // ক) নিজের সেলফ-রেজিস্ট্রেশন (সাধারণ সদস্য হিসেবে):
    (request.auth.uid == memberUid
      && request.resource.data.keys().hasOnly(['uid', 'email', 'displayName', 'role', 'joinedAt', 'status', 'addedBy'])
      && request.resource.data.uid == request.auth.uid
      && request.resource.data.email == request.auth.token.email
      && request.resource.data.role == 'member' // নিজে নিজে ম্যানেজার হতে পারবে না!
      && request.resource.data.status == 'active'
      && request.resource.data.joinedAt == request.time)
    ||
    // খ) বর্তমান ম্যানেজার নতুন সদস্য যুক্ত করছেন
    (isCurrentMessManager(messId) && request.resource.data.uid == memberUid)
  );

  // ২. Update রুল:
  allow update: if isSignedIn() && (
    // ক) মেম্বার নিজে শুধুমাত্র তার নাম ও ফোন বদলাতে পারে (রোল বা স্ট্যাটাস কখনো নয়)
    (request.auth.uid == memberUid
      && request.resource.data.uid == resource.data.uid
      && request.resource.data.email == resource.data.email
      && request.resource.data.role == resource.data.role
      && request.resource.data.status == resource.data.status
      && request.resource.data.joinedAt == resource.data.joinedAt
      && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['displayName', 'photoUrl', 'phone']))
    ||
    // খ) হ্যান্ডওভার ট্রানজাকশনের অংশ হিসেবে রোল আপডেট
    (isCurrentMessManager(messId)
      && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['role', 'status', 'updatedAt']))
  );

  // মেম্বার রিমুভ শুধুমাত্র বর্তমান ম্যানেজার করতে পারবেন
  allow delete: if isCurrentMessManager(messId);
}
```

---

## ৫. Initial Admin Bootstrap-এর নিরাপদ ব্যবস্থা (Sealed Production Instance)

### সমস্যা:
যদি ফায়ারস্টোর রুলসে নতুন মেস তৈরির অবাধ স্বাধীনতা থাকে (`allow create: if isSignedIn()`), তবে যেকোনো বহিরাগত ইউজার নতুন কোনো `messId` তৈরি করে নিজেকে সেটির সর্বময় কর্তা হিসেবে ঘোষণা করতে পারে অথবা এক্সিস্টিং মেস ওভাররাইট করার চেষ্টা করতে পারে।

### সমাধান (Spark Plan Friendly - Sealed Production Instance):
যেহেতু আমাদের প্রোডাকশন ডেটাবেজে `default_mess` ইতিমধ্যে সফলভাবে তৈরি ও সংরক্ষিত রয়েছে, তাই ক্লায়েন্ট অ্যাপ থেকে নতুন মেস রুট ডকুমেন্ট ক্রিয়েট করার সুযোগ সম্পূর্ণ বন্ধ করে দেওয়া হবে।

### ফায়ারস্টোর সিকিউরিটি রুলস লজিক:
```javascript
match /messes/{messId} {
  // মেস তৈরির সুযোগ সম্পূর্ণ বন্ধ (Sealed Production Instance):
  // কোনো সাধারণ ক্লায়েন্ট ফায়ারস্টোরে নতুন মেস বানাতে পারবে না
  allow create: if false;

  // মেস ডিলিট করা সম্পূর্ণ নিষিদ্ধ
  allow delete: if false;
  
  // রিড পারমিশন: অনুমোদিত সদস্য অথবা সক্রিয় ম্যানেজার
  allow read: if isMessAuthorized(messId);
  
  // আপডেট পারমিশন: শুধুমাত্র ধারা ৩ অনুযায়ী পারমাণবিক হ্যান্ডওভার বা বৈধ ম্যানেজার সেটিংস আপডেট
}
```
*নোট: যদি ভবিষ্যতে মাল্টি-মেস আর্কিটেকচার প্রয়োজন হয়, তবে সেখানে অ্যাডমিন সিক্রেট ইনভাইটেশন টোকেন দিয়ে ভেরিফাই করা যাবে। বর্তমান একক মেস অ্যাপ্লিকেশনের জন্য `allow create: if false;` শতভাগ আক্রমণ-প্রতিরোধী।*

---

## ৬. Android Keystore Key Recovery, Cross-Device Backup Restore এবং Firestore 1 MiB Document Limit-এর সমাধান

### ক) Android Keystore ও Cross-Device Restore সমস্যা ও সমাধান:
- **চ্যালেঞ্জ:** Android Hardware Keystore (`AndroidKeyStore`) চাবিটি নির্দিষ্ট ডিভাইসের হার্ডওয়্যার সিকিউর এলিমেন্ট (TEE/SE)-এ আবদ্ধ থাকে। যদি কোনো ম্যানেজার ফোন হারিয়ে ফেলেন বা নতুন ফোনে লগইন করেন, তবে আগের ফোনের Keystore কি দিয়ে এনক্রিপ্ট করা ক্লাউড ব্যাকআপ নতুন ফোনে ডিক্রিপ্ট করা অসম্ভব হয়ে পড়বে।
- **সমাধান (Hybrid Envelope Encryption Architecture):**
  1. **লোকাল ক্যাশ নিরাপত্তা:** ডিভাইসের লোকাল অফলাইন ক্যাশের জন্য Android Keystore ব্যবহার হবে।
  2. **ক্লাউড ব্যাকআপ নিরাপত্তা:** ব্যাকআপের জন্য ক্লাউড ডেটাবেজে ব্যাকআপ ফাইলটি ক্রিপ্টোগ্রাফিক **PBKDF2WithHmacSHA256** অ্যালগরিদমে তৈরি মেস রিকভারি সিক্রেট দিয়ে এনক্রিপ্ট হবে (অথবা ইন্টিগ্রিটি হ্যাশিং SHA-256 দিয়ে যাচাই হবে)। ব্যাকআপ মেটাডেটাতে সল্ট (Salt) ও ইনিশিয়ালাইজেশন ভেক্টর (IV) সংরক্ষিত থাকবে।
  3. ফলে যেকোনো ভেরিফাইড ম্যানেজার নতুন ডিভাইসে লগইন করে মেসের সিকিউরিটি কী বা গুগল রি-অথেন্টিকেশনের মাধ্যমে ক্লাউড থেকে ব্যাকআপ সফলভাবে রিস্টোর করতে পারবেন।

### খ) Firestore 1 MiB Document Limit সমস্যা ও সমাধান:
- **চ্যালেঞ্জ:** ফায়ারস্টোরের যেকোনো একক ডকুমেন্টের সর্বোচ্চ সাইজ সীমা হলো ১ মেগাবাইট (১,০৪৮,৫৭৬ বাইট)। কয়েক বছরের হাজার হাজার মিল রেকর্ড, বাজার খরচ, ইউটিলিটি বিল এবং অডিট লগ একটি একক ব্যাকআপ ডকুমেন্টে জেসন (JSON) স্ট্রিং হিসেবে পুশ করলে ১ MiB সীমা অতিক্রম করে ফায়ারস্টোর `RESOURCE_EXHAUSTED` বা `INVALID_ARGUMENT` এক্সেপশন দেবে।
- **সমাধান (Chunked Backup Architecture):**
  1. **মাস-ভিত্তিক ব্যাকআপ সেগমেন্টেশন:** ব্যাকআপগুলো মাসভিত্তিক পার্টিশনে সংরক্ষিত হবে (`backup_2026_11`, `backup_2026_10`)। একটি সাধারণ মেসে এক মাসে গড়ে ১৫০-৩০০ রেকর্ড থাকে, যার সাইজ মাত্র ৩০-৮০ কিলোবাইট (১ MiB-এর ৮% এরও কম)।
  2. **পূর্ণাঙ্গ সিস্টেম আর্কাইভ চাংকিং (`/backups/{backupId}/chunks/{chunkIndex}`):**
     যখন সম্পূর্ণ মেসের শুরু থেকে সব তথ্য একসাথে ব্যাকআপ নেওয়া হবে, তখন ক্লায়েন্ট ডেটা পে-লোডকে ৫০০ কিলোবাইটের ব্লকে স্বয়ংক্রিয়ভাবে ভাগ করবে:
     - প্যারেন্ট ডকুমেন্ট: `{ backupId, totalChunks: 3, totalRecords: 2400, sha256Checksum: "...", createdAt }`
     - চাইল্ড চাংকস:
       `/backups/{backupId}/chunks/0` (প্রথম ৫০০ KB)
       `/backups/{backupId}/chunks/1` (দ্বিতীয় ৫০০ KB)
       `/backups/{backupId}/chunks/2` (অবশিষ্টাংশ)
  3. রিস্টোরের সময় সব চাংক সিরিয়ালি ডাউনলোড করে জোড়া লাগিয়ে SHA-256 হ্যাশ মেলানো হবে, এরপর ট্রানজাকশনে রিস্টোর হবে।

---

## ৭. পুরোনো APK, Offline Writes এবং Rules Cutover Compatibility Policy

### ক) পুরোনো APK প্রতিরোধ নীতি (App Version Minimum Enforcement):
- ফায়ারস্টোরের `messes/{messId}` ডকুমেন্টে একটি ফিল্ড থাকবে `minRequiredVersionCode: 200`।
- ফায়ারস্টোর রুলসে হ্যান্ডওভার এবং সেনসিটিভ রাইট অপারেশনে চেক করা হবে:
  `request.resource.data.appVersionCode >= resource.data.minRequiredVersionCode`
- যদি কোনো ব্যবহারকারী পুরোনো আনপ্যাচড APK দিয়ে আগের পদ্ধতিতে ডাইরেক্ট ইমেইল পুশ বা পাসওয়ার্ড দিয়ে আপডেট করার চেষ্টা করে, ফায়ারস্টোর রুলস তা সরাসরি রিজেক্ট করবে।
- ব্যবহারকারীর স্ক্রিনে অ্যাপ আপডেট ডায়ালগ প্রদর্শিত হবে: *"মেসের নিরাপত্তা কাঠামো আপডেট করা হয়েছে। কাজ চালিয়ে যেতে অনুগ্রহ করে সর্বশেষ ভার্সনে আপডেট করুন।"*

### খ) অফলাইন রাইটস (Offline Writes) ও সিঙ্ক সামঞ্জস্যতা:
- **দৈনন্দিন রেকর্ড (মিল, বাজার, ডিম, ইউটিলিটি):**
  এসব ডেটা অফলাইনে ডিভাইসে ক্যাশ হবে। ডিভাইস অনলাইনে ফিরলে ফায়ারস্টোর SDK স্বয়ংক্রিয়ভাবে সিঙ্ক করবে। রুলসে এগুলোর ক্ষেত্রে শুধুমাত্র মেসের অনুমোদিত সদস্যপদ (`isMessAuthorized`) যাচাই করা হয়, যা অফলাইনে থাকা অবস্থায় কোনো জটিলতা তৈরি করবে না।
- **ম্যানেজার হ্যান্ডওভার অপারেশন:**
  হ্যান্ডওভার কখনো অফলাইনে সম্পন্ন হবে না। `db.runTransaction` এর অন্তর্নির্মিত বৈশিষ্ট্যই হলো এটি ইন্টারনেটের সাথে লাইভ সংযোগ ছাড়া এক্সিকিউট হয় না। ফলে অফলাইনে থাকা অবস্থায় কেউ ম্যানেজার হওয়ার চেষ্টা করতে পারবে না এবং কোনো ফ্যান্টম কনফ্লিক্ট সৃষ্টি হবে না।

### গ) জিরো-ডাউনটাইম রুলস কাট-ওভার পরিকল্পনা:
1. **ধাপ ১:** ক্লায়েন্ট অ্যাপ্লিকেশনে ইনভাইটেশন ও `authorized_members` সিঙ্কিং কোড সক্রিয় করা।
2. **ধাপ ২:** ফায়ারস্টোর রুলসে ব্যাকওয়ার্ড কমপ্যাটিবল রিড পারমিশন সহ নতুন ত্রিমুখী ভ্যালিডেশন চালু করা।
3. **ধাপ ৩:** সকল সক্রিয় সদস্যদের অটোমেটিক প্রোফাইল সিঙ্ক নিশ্চিত হওয়ার পর লিগ্যাসি রাইট পাথ সম্পূর্ণ বন্ধ করে দেওয়া।

---

## ৮. নিরাপত্তা সীমাবদ্ধতা: যেসব বিষয় শুধু Firestore Rules দিয়ে সমাধান করা সম্ভব নয়

স্বচ্ছতা ও স্থাপত্যের নিরাপত্তার স্বার্থে নিচের বিষয়গুলো জেনে রাখা আবশ্যক, যা ফায়ারস্টোর রুলসের এখতিয়ারের বাইরে এবং কীভাবে সেগুলোর সমাধান অ্যাপ কোডে করতে হবে:

1. **ইমেইল ডেলিভারি ও বাহ্যিক মেলবক্স সত্যতা:** ফায়ারস্টোর রুলস শুধু দেখতে পারে সাইন-ইন করা ব্যবহারকারীর জিমেইলটি গুগল কর্তৃক ভেরিফাইড কিনা (`request.auth.token.email_verified == true`)। তবে কোনো নির্দিষ্ট ইনবক্সে মেইল আসলেই পৌঁছেছে কিনা তা ফায়ারস্টোর রুলস যাচাই করতে পারে না। (এজন্যই আমাদের ইনভাইটেশন হ্যান্ডশেক সিস্টেম ইন-অ্যাপ ফায়ারস্টোর কোয়েরি ব্যবহার করে কাজ করে)।
2. **রেট-লিমিটিং (Rate Limiting / DDoS Protection):** ফায়ারস্টোর রুলস কোনো নির্দিষ্ট IP বা ক্লায়েন্টের সেকেন্ডে কয়টি Read রিকোয়েস্ট হচ্ছে তা মেপে স্প্যামিং আটকাতে পারে না। এর সমাধান ক্লায়েন্ট-সাইড ডেবাউন্সিং (Debouncing) এবং ফায়ারবেস অ্যাপ চেক (Firebase App Check - Play Integrity)-এর মাধ্যমে নিশ্চিত করা হয়।
3. **ডিভাইসের ফিজিক্যাল মেমোরি ও রুট প্রোটেকশন:** ব্যবহারকারীর ফোন যদি রুটেড (Rooted) হয় এবং র‍্যাম হ্যাকিং টুল ব্যবহার করা হয়, তবে রুলস তা প্রতিরোধ করতে পারে না। এজন্য সংবেদনশীল ডেটা ট্রানজিটে এনক্রিপ্ট রাখা হয় এবং সার্ভার সাইড ভ্যালিডেশনই চূড়ান্ত সত্য হিসেবে গৃহীত হয়।
4. **ব্যাকগ্রাউন্ড ক্রন জব বা টাইম-বেজড অটোমেশন:** ফায়ারস্টোর রুলস কোনো ইভেন্ট-ট্রিগার ছাড়া নির্দিষ্ট সময়ে একা একা রান হতে পারে না। ফলে ইনভাইটেশন মেয়াদোত্তীর্ণ করার ফিল্টার ক্লায়েন্ট কুয়েরি এবং রুলসে `request.time < resource.data.expiresAt` চেকের মাধ্যমে তাৎক্ষণিকভাবে প্রয়োগ করা হয়।

---

## ৯. ভেরিফিকেশন ও টেস্ট স্ট্র্যাটেজি (পরবর্তী ইমপ্লিমেন্টেশন পর্বের জন্য)

পরবর্তী ধাপে কোড ও রুলস পরিবর্তনের পর কোনো টেস্ট না চালিয়ে PASS ঘোষণা করা হবে না। নিচের নির্ধারিত পরীক্ষাগুলো সম্পাদন করা হবে:
1. **Robolectric JVM Tests (`gradle :app:testDebugUnitTest`):** মেস ক্যালকুলেটর, ডেটা রূপান্তর, এবং পারমাণবিক হ্যান্ডওভার ইনভাইটেশন লজিকের লোকাল টেস্ট।
2. **Firestore Security Rules Unit Tests:** অনুমোদিত ও অননুমোদিত ইউজারের জন্য রুলস ডিনায়েল এবং পারমাণবিক ত্রিমুখী ভ্যালিডেশন টেস্ট।
3. **Applet Compilation (`compile_applet`):** সম্পূর্ণ অ্যান্ড্রয়েড কোডের কম্পাইলেশন ও এরর-ফ্রি বিল্ড ভেরিফিকেশন।
4. **ডিপ্লয়মেন্ট ভেরিফিকেশন (`deploy_firebase`):** লাইভ ফায়ারস্টোরে রুলসের সিনট্যাক্স যাচাই ও সফল ডিপ্লয়।

---
**পরিকল্পনা প্রস্তুত সমাপ্ত। কোনো সোর্স কোড বা কনফিগারেশন পরিবর্তন করা হয়নি। আপনার চূড়ান্ত সবুজ সংকেতের পর ইমপ্লিমেন্টেশন শুরু করা হবে।**

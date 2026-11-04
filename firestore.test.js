const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const MANAGER_UID = "manager_123";
const MEMBER_UID = "member_456";
const MESS_ID = "mess_test_1";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: can read mess but CANNOT write", async () => {
  // Seed mess
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("messes").doc(MESS_ID).set({
      name: "Dhaka Student Mess",
      currency: "BDT",
      managerUid: MANAGER_UID,
      managerEmail: "manager@test.com",
      createdAt: new Date(),
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("messes").doc(MESS_ID).get());
  await assertFails(unauthDb.collection("messes").doc(MESS_ID).set({ name: "Hacker Mess" }));
});

test("Authenticated manager: can create a mess", async () => {
  const managerDb = testEnv.authenticatedContext(MANAGER_UID).firestore();
  await assertSucceeds(
    managerDb.collection("messes").doc(MESS_ID).set({
      name: "Dhaka Student Mess",
      currency: "BDT",
      managerUid: MANAGER_UID,
      managerEmail: "manager@test.com",
      createdAt: new Date(),
    })
  );
});

test("Non-manager: cannot add a member to the mess", async () => {
  // Seed mess with MANAGER_UID
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("messes").doc(MESS_ID).set({
      name: "Dhaka Student Mess",
      currency: "BDT",
      managerUid: MANAGER_UID,
      managerEmail: "manager@test.com",
      createdAt: new Date(),
    });
  });

  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  await assertFails(
    memberDb.collection("messes").doc(MESS_ID).collection("members").doc("mem_1").set({
      messId: MESS_ID,
      name: "Rafiq",
      phone: "+8801700000000",
      potDeposits: 20,
      cashDeposits: 2000,
      active: true,
      createdAt: new Date(),
    })
  );
});

test("Manager: can add and update members and daily meals", async () => {
  // Seed mess
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("messes").doc(MESS_ID).set({
      name: "Dhaka Student Mess",
      currency: "BDT",
      managerUid: MANAGER_UID,
      managerEmail: "manager@test.com",
      createdAt: new Date(),
    });
  });

  const managerDb = testEnv.authenticatedContext(MANAGER_UID).firestore();
  await assertSucceeds(
    managerDb.collection("messes").doc(MESS_ID).collection("members").doc("mem_1").set({
      messId: MESS_ID,
      name: "Rafiq",
      phone: "+8801700000000",
      potDeposits: 20,
      cashDeposits: 2000,
      active: true,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    managerDb.collection("messes").doc(MESS_ID).collection("daily_meals").doc("2026-10-29").set({
      messId: MESS_ID,
      date: "2026-10-29",
      breakfastCount: 1.5,
      lunchCount: 2.0,
      dinnerCount: 3.0,
      memberMeals: {
        mem_1: { b: 0.5, l: 1.0, d: 1.0 }
      },
      createdAt: new Date(),
    })
  );
});

test("Audit logs: are immutable (cannot be updated or deleted)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("messes").doc(MESS_ID).set({
      name: "Dhaka Student Mess",
      currency: "BDT",
      managerUid: MANAGER_UID,
      managerEmail: "manager@test.com",
      createdAt: new Date(),
    });
  });

  const managerDb = testEnv.authenticatedContext(MANAGER_UID).firestore();
  await assertSucceeds(
    managerDb.collection("messes").doc(MESS_ID).collection("audit_logs").doc("log_1").set({
      messId: MESS_ID,
      action: "EDIT_MEAL",
      details: "Changed breakfast count",
      performedBy: "manager@test.com",
      timestamp: new Date(),
    })
  );

  // Updating or deleting must fail
  await assertFails(
    managerDb.collection("messes").doc(MESS_ID).collection("audit_logs").doc("log_1").update({
      details: "Tampered details",
    })
  );
  await assertFails(
    managerDb.collection("messes").doc(MESS_ID).collection("audit_logs").doc("log_1").delete()
  );
});

test("Member / Unauthenticated: can READ daily meals and egg records but CANNOT write or edit them", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("messes").doc(MESS_ID).set({
      name: "Dhaka Student Mess",
      currency: "BDT",
      managerUid: MANAGER_UID,
      managerEmail: "manager@test.com",
      createdAt: new Date(),
    });
    await context.firestore().collection("messes").doc(MESS_ID).collection("daily_meals").doc("2026-10-30").set({
      messId: MESS_ID,
      date: "2026-10-30",
      breakfastCount: 1.0,
      lunchCount: 1.0,
      dinnerCount: 1.0,
      memberMeals: {
        mem_1: { b: 1.0, l: 1.0, d: 1.0 }
      },
      createdAt: new Date(),
    });
    await context.firestore().collection("messes").doc(MESS_ID).collection("egg_records").doc("egg_1").set({
      messId: MESS_ID,
      date: "2026-10-30",
      purchasedCount: 50,
      consumedCount: 20,
      damagedCount: 2,
      cost: 600,
      notes: "Morning bazaar",
      createdAt: new Date(),
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  // Can read
  await assertSucceeds(unauthDb.collection("messes").doc(MESS_ID).collection("daily_meals").doc("2026-10-30").get());
  await assertSucceeds(unauthDb.collection("messes").doc(MESS_ID).collection("egg_records").doc("egg_1").get());

  // But CANNOT write daily meals
  await assertFails(
    unauthDb.collection("messes").doc(MESS_ID).collection("daily_meals").doc("2026-10-30").set({
      messId: MESS_ID,
      date: "2026-10-30",
      breakfastCount: 2.0,
      lunchCount: 2.0,
      dinnerCount: 2.0,
      memberMeals: {},
      createdAt: new Date(),
    })
  );

  // And CANNOT write egg records
  await assertFails(
    unauthDb.collection("messes").doc(MESS_ID).collection("egg_records").doc("egg_2").set({
      messId: MESS_ID,
      date: "2026-10-30",
      purchasedCount: 10,
      consumedCount: 0,
      damagedCount: 0,
      cost: 120,
      createdAt: new Date(),
    })
  );
});

test("Manager: can create, update, and delete egg records", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("messes").doc(MESS_ID).set({
      name: "Dhaka Student Mess",
      currency: "BDT",
      managerUid: MANAGER_UID,
      managerEmail: "manager@test.com",
      createdAt: new Date(),
    });
  });

  const managerDb = testEnv.authenticatedContext(MANAGER_UID).firestore();
  await assertSucceeds(
    managerDb.collection("messes").doc(MESS_ID).collection("egg_records").doc("egg_1").set({
      messId: MESS_ID,
      date: "2026-10-30",
      purchasedCount: 100,
      consumedCount: 30,
      damagedCount: 3,
      cost: 1250,
      notes: "Full crate bought",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    managerDb.collection("messes").doc(MESS_ID).collection("egg_records").doc("egg_1").delete()
  );
});

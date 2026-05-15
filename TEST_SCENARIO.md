# Test Scenario #1: Real-World Pattern Detection

## Objective
Validate that the AI Agent can detect repetitive user actions across different applications and display suggestions.

## Prerequisites
- Device with Android 8+ (API 26+)
- APK installed on device
- ADB connected for logcat monitoring
- Telegram installed
- WhatsApp installed (optional)

## Setup Phase

### 1. Initial Configuration
```bash
# Connect device and verify ADB
adb devices

# Clear previous logs
adb logcat -c

# Start logcat monitoring
adb logcat | grep -E "MainActivity|OverlayService|AccessibilityService"
```

### 2. App Setup
1. Open AI Agent app
2. Grant overlay permission (if prompted)
3. Enable accessibility service:
   - Tap "Enable Accessibility" button
   - Find "AI Agent" in accessibility settings
   - Enable it
4. Enter API key (optional for basic testing)
5. Enable "Tracking" switch
6. Tap "Start Service" button

### 3. Verify Setup
- Check logcat for: `MainActivity: Start button clicked - initiating service start`
- Check logcat for: `OverlayService: onCreate: OverlayService starting`
- Check logcat for: `AccessibilityService: onServiceConnected: AccessibilityService connected`
- Verify notification appears in status bar
- Verify "AI" overlay button appears in top-right corner

---

## Test Case 1: Telegram Pattern Detection

### Step 1: Open Telegram
**Action**: Open Telegram app from home screen

**Expected Logs**:
```
AccessibilityService: Package changed: null -> org.telegram.messenger
AccessibilityService: Event: SCREEN_CHANGED | Package: org.telegram.messenger
```

**Verification**:
- [ ] Log shows package name changed to `org.telegram.messenger`
- [ ] Overlay button is still visible
- [ ] Notification is still present

### Step 2: Navigate to Chats Tab
**Action**: Tap on "Chats" tab

**Expected Logs**:
```
AccessibilityService: Event: CLICK | Package: org.telegram.messenger
AccessibilityService: Event: SCREEN_CHANGED | Package: org.telegram.messenger
```

**Verification**:
- [ ] CLICK event logged
- [ ] SCREEN_CHANGED event logged
- [ ] UI context captured

### Step 3: Repeat Action 3 Times
**Action**: Tap on the same chat 3 times

**Expected Logs** (for each tap):
```
AccessibilityService: Event: CLICK | Package: org.telegram.messenger
AccessibilityService: Event: SCREEN_CHANGED | Package: org.telegram.messenger
```

**Verification**:
- [ ] 3 CLICK events logged
- [ ] 3 SCREEN_CHANGED events logged
- [ ] All events have same package name

### Step 4: Type Message 3 Times
**Action**: Type "test" in chat input, send, repeat 3 times

**Expected Logs** (for each message):
```
AccessibilityService: Event: TEXT_INPUT | Package: org.telegram.messenger
AccessibilityService: Event: CLICK | Package: org.telegram.messenger
```

**Verification**:
- [ ] TEXT_INPUT events logged
- [ ] CLICK events logged (send button)
- [ ] Text content captured

### Step 5: Check for Suggestion
**Action**: Wait 5-10 seconds after 3rd repeat

**Expected Behavior**:
- Suggestion overlay appears at bottom of screen
- Shows: "org.telegram.messenger • CLICK x3" or similar

**Expected Logs**:
```
OverlayService: showSuggestion: Showing suggestion for org.telegram.messenger - CLICK x3
```

**Verification**:
- [ ] Suggestion panel appears
- [ ] Shows correct package name
- [ ] Shows correct action type
- [ ] Shows correct repeat count
- [ ] "Yes" and "No" buttons are visible

### Step 6: Test Suggestion Interaction
**Action**: Tap "Yes" button

**Expected Logs**:
```
OverlayService: Suggestion accepted by user
AccessibilityService: Event: CLICK | Package: org.telegram.messenger | Context: ... | Text: SUGGESTION_ACCEPTED
```

**Verification**:
- [ ] Suggestion dismissed
- [ ] SUGGESTION_ACCEPTED event logged
- [ ] Overlay button still visible

---

## Test Case 2: WhatsApp Pattern Detection (Optional)

### Step 1: Switch to WhatsApp
**Action**: Open WhatsApp app

**Expected Logs**:
```
AccessibilityService: Package changed: org.telegram.messenger -> com.whatsapp
AccessibilityService: Event: SCREEN_CHANGED | Package: com.whatsapp
```

**Verification**:
- [ ] Package name changed to `com.whatsapp`
- [ ] Overlay button still visible
- [ ] Previous Telegram patterns not affecting WhatsApp

### Step 2: Repeat Navigation Pattern
**Action**: Tap on "Chats" tab 3 times

**Expected Logs**:
```
AccessibilityService: Event: CLICK | Package: com.whatsapp
```

**Verification**:
- [ ] 3 CLICK events logged
- [ ] All events have package `com.whatsapp`
- [ ] Separate pattern detection for WhatsApp

### Step 3: Check for Suggestion
**Action**: Wait 5-10 seconds

**Verification**:
- [ ] Suggestion appears for WhatsApp
- [ ] Shows `com.whatsapp` package name
- [ ] No interference from Telegram patterns

---

## Test Case 3: Settings App Pattern Detection

### Step 1: Open Settings
**Action**: Open Android Settings app

**Expected Logs**:
```
AccessibilityService: Package changed: com.whatsapp -> com.android.settings
AccessibilityService: Event: SCREEN_CHANGED | Package: com.android.settings
```

**Verification**:
- [ ] Package name changed to `com.android.settings`
- [ ] Overlay button still visible over Settings

### Step 2: Navigate to Wi-Fi Settings
**Action**: Tap "Network & Internet" -> "Wi-Fi" (3 times)

**Expected Logs**:
```
AccessibilityService: Event: CLICK | Package: com.android.settings
AccessibilityService: Event: SCREEN_CHANGED | Package: com.android.settings
```

**Verification**:
- [ ] CLICK events logged
- [ ] SCREEN_CHANGED events logged
- [ ] UI context includes view IDs

### Step 3: Tap on Network 3 Times
**Action**: Tap on the same Wi-Fi network 3 times

**Expected Logs**:
```
AccessibilityService: Event: CLICK | Package: com.android.settings
```

**Verification**:
- [ ] 3 CLICK events logged
- [ ] Pattern detected for Settings app

### Step 4: Check for Suggestion
**Action**: Wait 5-10 seconds

**Verification**:
- [ ] Suggestion appears for Settings
- [ ] Shows `com.android.settings` package name
- [ ] Overlay visible over Settings app

---

## Test Case 4: Overlay Persistence

### Step 1: Press Home Button
**Action**: Press home button while in any app

**Verification**:
- [ ] Overlay button remains visible on home screen
- [ ] Notification still present
- [ ] No crash in logcat

### Step 2: Switch Between Apps
**Action**: Use recent apps to switch between Telegram, WhatsApp, Settings

**Verification**:
- [ ] Overlay button persists across app switches
- [ ] Package name changes logged correctly
- [ ] No overlay disappearance

### Step 3: Lock and Unlock Screen
**Action**: Lock device, then unlock

**Verification**:
- [ ] Overlay button reappears after unlock
- [ ] Service still running (check notification)
- [ ] No service restart in logs

---

## Test Case 5: Service Stability

### Step 1: Monitor Service for 5 Minutes
**Action**: Leave app running, monitor logcat

**Verification**:
- [ ] No service crash logs
- [ ] No repeated service restarts
- [ ] Memory usage stable (check with `adb shell dumpsys meminfo com.androidaiagent`)

### Step 2: Background the App
**Action**: Press home, open other apps, use phone normally for 2 minutes

**Verification**:
- [ ] Service continues running
- [ ] Notification remains
- [ ] Accessibility service still active
- [ ] Events still logged when switching apps

---

## Expected Runtime Issues (Document These)

### Overlay Permission Issues
**Symptoms**:
- Overlay doesn't appear
- Log: `OverlayService: Cannot create overlay - permission not granted`

**Resolution**:
- Go to Settings > Apps > Special Access > Display over other apps
- Enable for AI Agent

### Accessibility Service Disabled
**Symptoms**:
- No events logged
- Log: `AccessibilityService: onInterrupt: AccessibilityService interrupted`

**Resolution**:
- Re-enable in accessibility settings
- Check battery optimization settings
- Add to allowed apps in background restrictions

### Service Killed by System
**Symptoms**:
- Notification disappears
- Overlay disappears
- Log: Service restart messages

**Resolution**:
- Disable battery optimization for app
- Add to background allowed apps
- Check Android version-specific restrictions

---

## Success Criteria

Test scenario passes when:
- ✅ All 3 critical validations pass (Service, Overlay, Accessibility)
- ✅ Pattern detection works on at least 2 apps
- ✅ Suggestion overlay appears correctly
- ✅ Overlay persists across app switches and screen lock
- ✅ Service remains stable for 5+ minutes
- ✅ Package name changes logged correctly
- ✅ No crashes or unexpected service restarts

---

## Logcat Commands Reference

### Monitor All Services
```bash
adb logcat | grep -E "MainActivity|OverlayService|AccessibilityService"
```

### Monitor Package Changes Only
```bash
adb logcat | grep "Package changed"
```

### Monitor Events Only
```bash
adb logcat | grep "Event:"
```

### Monitor Errors
```bash
adb logcat *:E
```

### Save Logs to File
```bash
adb logcat > test_scenario_logs.txt
```

---

## Next Steps After Validation

If all tests pass:
1. Test on different Android versions (11, 12, 13, 14)
2. Test on different device manufacturers (Samsung, Xiaomi, Pixel)
3. Test with more complex app interactions
4. Implement action replay layer (without AI)
5. Add more pattern detection types

If tests fail:
1. Document specific failure mode
2. Check Android version compatibility
3. Review permission grants
4. Add more debug logging as needed
5. Fix identified issues before proceeding

# QA Testing Checklist - Android AI Agent MVP

## Critical Validation #1: Start Service

### Pre-conditions
- [ ] App installed on device
- [ ] App launched
- [ ] API key entered (optional for basic testing)

### Test Steps
1. [ ] Tap "Start Service" button
2. [ ] Verify notification appears in status bar
3. [ ] Verify notification shows "AI Agent running"
4. [ ] Wait 30 seconds - verify service doesn't crash
5. [ ] Pull down notification shade - verify notification is persistent
6. [ ] Tap notification - verify it opens MainActivity
7. [ ] Check logcat for service lifecycle events

### Expected Results
- ✅ Notification appears immediately
- ✅ Notification is ongoing (cannot be swiped away)
- ✅ Service stays alive in background
- ✅ No crash logs in logcat

### Failure Modes
- ❌ No notification appears
- ❌ Notification disappears after few seconds
- ❌ Service crashes (check logcat)
- ❌ Notification can be swiped away

---

## Critical Validation #2: Overlay

### Pre-conditions
- [ ] Overlay permission granted (Settings > Apps > Special Access > Display over other apps)
- [ ] Service is running

### Test Steps
1. [ ] Verify "AI" button appears in top-right corner
2. [ ] Open any other app (e.g., Telegram)
3. [ ] Verify overlay button is still visible
4. [ ] Press Home button
5. [ ] Verify overlay button is still visible
6. [ ] Open recent apps and switch between apps
7. [ ] Verify overlay button persists across app switches
8. [ ] Tap overlay button - verify it opens MainActivity
9. [ ] Lock screen, unlock, verify overlay reappears

### Expected Results
- ✅ Overlay button appears immediately after service start
- ✅ Overlay stays on top of any app
- ✅ Overlay persists through app switches
- ✅ Overlay persists after screen lock/unlock
- ✅ Overlay button is clickable

### Failure Modes
- ❌ Overlay doesn't appear
- ❌ Overlay disappears when switching apps
- ❌ Overlay disappears after screen lock
- ❌ Overlay is not clickable
- ❌ Overlay appears behind other apps

---

## Critical Validation #3: Accessibility

### Pre-conditions
- [ ] Accessibility service enabled (Settings > Accessibility > AI Agent)
- [ ] Tracking switch is ON in MainActivity
- [ ] Service is running

### Test Steps
1. [ ] Open logcat and filter by "AccessibilityService"
2. [ ] Open Telegram
3. [ ] Verify packageName logged as "org.telegram.messenger"
4. [ ] Tap a chat in Telegram
5. [ ] Verify CLICK event logged
6. [ ] Type a message
7. [ ] Verify TEXT_INPUT event logged
8. [ ] Press Back
9. [ ] Verify SCREEN_CHANGED event logged
10. [ ] Open WhatsApp
11. [ ] Verify packageName changes to "com.whatsapp"
12. [ ] Open Settings
13. [ ] Verify packageName changes to "com.android.settings"

### Expected Results
- ✅ packageName changes correctly when switching apps
- ✅ Different event types logged (CLICK, TEXT_INPUT, SCREEN_CHANGED)
- ✅ UI context captured (className, viewId, text)
- ✅ Events logged in real-time (no significant delay)

### Failure Modes
- ❌ packageName doesn't change
- ❌ No events logged
- ❌ Service disabled by system
- ❌ Events logged with delay

---

## Runtime Issue Detection

### Overlay Permission
- [ ] Test on Android 12+ (requires explicit grant)
- [ ] Test on Android 11 (different permission model)
- [ ] Verify permission request dialog appears
- [ ] Verify graceful handling if permission denied

### Accessibility Service
- [ ] Verify service doesn't get killed by system
- [ ] Test after device reboot
- [ ] Test after app update
- [ ] Check battery optimization settings

### Background Service
- [ ] Test on Android 8+ (background limits)
- [ ] Test on Android 12+ (foreground service restrictions)
- [ ] Verify service survives Doze mode
- [ ] Check if service killed by battery optimization

### File Storage (Android 11+)
- [ ] Verify pattern data persists
- [ ] Test on Android 11 (scoped storage)
- [ ] Test on Android 13 (more restrictions)
- [ ] Check if data survives app restart

---

## Test Scenario #1: Telegram/WhatsApp/Settings

### Setup
1. [ ] Install app on device
2. [ ] Grant overlay permission
3. [ ] Enable accessibility service
4. [ ] Enter API key (optional)
5. [ ] Enable tracking switch
6. [ ] Start service

### Telegram Test
1. [ ] Open Telegram
2. [ ] Navigate to Chats tab
3. [ ] Tap on a chat (3 times)
4. [ ] Type "test" message (3 times)
5. [ ] Press Back
6. [ ] Verify overlay suggestion appears after 3rd repeat

### WhatsApp Test
1. [ ] Open WhatsApp
2. [ ] Navigate to Chats tab
3. [ ] Tap on a chat (3 times)
4. [ ] Type "test" message (3 times)
5. [ ] Press Back
6. [ ] Verify overlay suggestion appears

### Settings Test
1. [ ] Open Settings
2. [ ] Navigate to Wi-Fi (3 times)
3. [ ] Tap on a network (3 times)
4. [ ] Press Back
5. [ ] Verify overlay suggestion appears

### Verification
- [ ] Check logcat for all events
- [ ] Verify pattern detection works
- [ ] Verify suggestion overlay appears
- [ ] Test "Yes" button on suggestion
- [ ] Test "No" button on suggestion

---

## Known Issues to Monitor

### Android Version Specific
- **Android 14**: May require explicit notification permission
- **Android 13**: Stricter overlay permissions
- **Android 12**: Foreground service start restrictions
- **Android 11**: Scoped storage affects data persistence

### Device Specific
- **Samsung**: May kill services aggressively (check battery settings)
- **Xiaomi**: Requires additional permissions for overlay
- **OnePlus**: May have different accessibility behavior
- **Google Pixel**: Usually most permissive

---

## Logcat Commands

### Filter by Service
```bash
adb logcat | grep -E "OverlayService|AccessibilityService|MainActivity"
```

### Filter by Package
```bash
adb logcat | grep "com.androidaiagent"
```

### Filter by Errors
```bash
adb logcat *:E
```

### Clear Log
```bash
adb logcat -c
```

---

## Success Criteria

MVP is considered validated when:
- ✅ All 3 critical validations pass
- ✅ Test scenario #1 completes without crashes
- ✅ Pattern detection works on at least 2 apps
- ✅ Overlay persists across app switches
- ✅ Accessibility service stays enabled for 24 hours

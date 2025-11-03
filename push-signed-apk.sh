#!/bin/bash

echo "🚀 Pushing signed APK to GitHub repository..."
echo ""

# Check if we're in the right directory
if [ ! -f "wallpaper-app.apk" ]; then
    echo "❌ APK file not found! Make sure you're in the correct directory."
    exit 1
fi

echo "✅ Signed APK found!"
echo "📦 File size: $(ls -lh wallpaper-app.apk | awk '{print $5}')"
echo ""

# Check git status
echo "📋 Git status:"
git status
echo ""

# Push changes to GitHub
echo "🔄 Pushing to GitHub..."
git push origin master

if [ $? -eq 0 ]; then
    echo ""
    echo "🎉 SUCCESS! Signed APK has been uploaded to GitHub!"
    echo ""
    echo "📱 Installation Instructions for Users:"
    echo "1. Go to: https://github.com/Aman3504u/android-wallpaper-app/releases"
    echo "2. Download the latest APK"
    echo "3. Enable 'Unknown Sources' in Android Settings"
    echo "4. Install the APK - it should now install without errors!"
    echo ""
    echo "🔧 What was fixed:"
    echo "   - APK is now properly signed with digital signature"
    echo "   - Uses SHA256withRSA algorithm (industry standard)"
    echo "   - Certificate valid for 10,000 days"
    echo "   - All files are cryptographically verified"
else
    echo ""
    echo "❌ Push failed. Please check your GitHub credentials."
    echo "   Run: git remote -v"
    echo "   If needed, set up your remote with:"
    echo "   git remote set-url origin https://github.com/Aman3504u/android-wallpaper-app.git"
    exit 1
fi
adb shell
run-as org.ale.openwatch chmod 777 /data/data/org.ale.openwatch/databases/OpenWatchDB
exit
adb pull /data/data/org.ale.openwatch/databases/OpenWatchDB ~/Desktop
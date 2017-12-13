# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Projects\Java\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# android support classes
-keep class android.support.v7.widget.SearchView { *; }

-keep public class com.cactusteam.money.data.dao.** { *;}

-keep public class com.google.android.gms.analytics.** {
    public *;
}

-dontwarn org.apache.poi.**
-dontwarn org.apache.commons.compress.**

-dontwarn java.beans.**

-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
-keep class org.codehaus.** { *; }
-keepclassmembers public final enum org.codehaus.jackson.annotate.JsonAutoDetect$Visibility {
    public static final org.codehaus.jackson.annotate.JsonAutoDetect$Visibility *;
}
-keep class com.cactusteam.money.sync.model.** { *; }
-keep class com.cactusteam.money.sync.changes.** { *; }

-keep class com.woxthebox.draglistview.** { *; }

# ui chart
-dontwarn com.github.mikephil.charting.data.realm.**
-keep class com.github.mikephil.charting.** { *; }

# rxjava
-dontwarn rx.internal.util.unsafe.**
-keep class rx.schedulers.Schedulers {
    public static <methods>;
}
-keep class rx.schedulers.ImmediateScheduler {
    public <methods>;
}
-keep class rx.schedulers.TestScheduler {
    public <methods>;
}
-keep class rx.schedulers.Schedulers {
    public static ** test();
}
-keepclassmembers class rx.internal.util.unsafe.** {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    long producerNode;
    long consumerNode;
}

# dropbox
-dontwarn okio.**
-dontwarn com.google.appengine.**
-dontwarn javax.servlet.**
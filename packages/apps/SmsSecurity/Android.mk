
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_JAVA_LIBRARIES := telephony-common \
                        mediatek-framework \
                        mediatek-telephony-common \
                    	CustomProperties
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v13

LOCAL_PACKAGE_NAME := SmsSecurity
LOCAL_CERTIFICATE := platform


include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(TOPDIR)/hardware/libhardware_legacy/include \
					$(TOPDIR)/hardware/libhardware/include

LOCAL_MODULE := smart_battery.default
LOCAL_MODULE_RELATIVE_PATH := hw

LOCAL_SRC_FILES := smart_battery.c
LOCAL_SHARED_LIBRARIES := liblog
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY) 

# Copyright 2006-2014 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= serial_test.c 

LOCAL_MODULE := serial_test

#LOCAL_CFLAGS := -Werror

include $(BUILD_EXECUTABLE)

include $(call first-makefiles-under,$(LOCAL_PATH))

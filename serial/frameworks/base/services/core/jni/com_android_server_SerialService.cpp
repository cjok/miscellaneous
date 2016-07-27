/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "SerialServiceJNI"
#include "utils/Log.h"

#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <hardware/hardware.h>
#include <hardware/serial.h>

namespace android
{

static struct parcel_file_descriptor_offsets_t
{
    jclass mClass;
    jmethodID mConstructor;
} gParcelFileDescriptorOffsets;

static jobject android_server_SerialService_open(JNIEnv *env, jobject thiz, jstring path)
{
    const char *pathStr = env->GetStringUTFChars(path, NULL);

    int fd = open(pathStr, O_RDWR | O_NOCTTY);
    if (fd < 0) {
        ALOGE("could not open %s", pathStr);
        env->ReleaseStringUTFChars(path, pathStr);
        return NULL;
    }
    env->ReleaseStringUTFChars(path, pathStr);

    jobject fileDescriptor = jniCreateFileDescriptor(env, fd);
    if (fileDescriptor == NULL) {
        return NULL;
    }
    return env->NewObject(gParcelFileDescriptorOffsets.mClass,
        gParcelFileDescriptorOffsets.mConstructor, fileDescriptor);
}

static serial_control_device_t *sSerialDevice = NULL;
static serial_module_t* sSerialModule=NULL;

static inline int serial_control_open(const struct hw_module_t* module,
		serial_control_device_t** device) {
	ALOGD("%s E", __func__);
	return module->methods->open(module,
			RSERIAL_HARDWARE_MODULE_ID, (struct hw_device_t**)device);
}

static jboolean android_server_SerialService_serial_init(JNIEnv *env, jobject thiz)
{
	ALOGD("%s E", __func__);
	serial_module_t * module = NULL;
	int ret;

	ret = hw_get_module(RSERIAL_HARDWARE_MODULE_ID, (hw_module_t const**)&module);
	if (ret == 0) {
		ALOGD(" get module OK");
		sSerialModule = (serial_module_t *) module;
		if (serial_control_open(&module->common, &sSerialDevice) != 0) 
		{
			ALOGE("serial_control_open error");
			return false;
		}
	} else {
		ALOGE("%s : ret = %d, error\n", __func__, ret);
		return false;
	}

	ALOGD("%s X", __func__);
	return true;

}
static jint android_server_SerialService_serial_set_control_speed_config(JNIEnv* env, jobject thiz, 
	jint speed_limit, jint mode) {
	ALOGD("%s E", __func__);
	int ret;

	if (sSerialDevice) {
		ret = sSerialDevice->serial_control_info(sSerialDevice,TYPE_CONTROL_SET_SPEED_CONFIG, speed_limit, mode, NULL);
	}else{
		ALOGE("sSerialDevice is null");
	}

	ALOGD("%s ret = %d X", __func__, ret);

	return ret;
}

static jint android_server_SerialService_serial_set_control_sync_status(JNIEnv* env, jobject thiz, 
	jint status, jint angle) {
	ALOGD("%s E", __func__);
	int ret;

	if (sSerialDevice) {
		ret = sSerialDevice->serial_control_info(sSerialDevice,TYPE_CONTROL_SYNC_STATUS, status, angle, NULL);
	}else{
		ALOGE("sSerialDevice is null");
	}

	ALOGD("%s ret = %d X", __func__, ret);

	return ret;
}

/*
static jint android_server_SerialService_serial_get_control_speed_config(JNIEnv* env, jobject thiz, jintArray data) 
{
	ALOGD("%s E", __func__);
	int ret;
	int *buf;

	buf = (int *)env->GetIntArrayElements(data,NULL);

	if (sSerialDevice) {
		ret = sSerialDevice->serial_control_info(sSerialDevice, TYPE_CONTROL_GET_SPEED_CONFIG, 0, 0, buf);
	}else{
		ALOGE("sSerialDevice is null");
	}

	ALOGE("buf[0] = 0x%x, buf[1] = 0x%x\n", buf[0], buf[1]);

	env->ReleaseIntArrayElements(data, (jint*)buf, 0);
	ALOGE("data[0] = 0x%x, data[1] = 0x%x\n", data[0], data[1]);
	ALOGD("%s X", __func__);
	return ret;
}*/

/*
* return value: 
* a[0]: error code , a[1]: controller error, a[2]: battery error
* a[3]: motor error , a[4]: communication error, a[5][6]: others error
*
*/
static jintArray android_server_SerialService_serial_get_traffic_status(JNIEnv* env, jobject thiz) 
{
	ALOGD("%s E", __func__);
	int ret;
	int buf[7] = {-1};
	jintArray arr;

	if (sSerialDevice) {
		ret = sSerialDevice->serial_control_info(sSerialDevice, TYPE_CONTROL_GET_TRAFFIC_STATUS,
				0, 0, &buf[1]);
		if (ret != -1)
			buf[0] = 0;
	}else{
		ALOGE("sSerialDevice is null");
	}

	ALOGE("buf[0] = %d, buf[1] = 0x%x, buf[2] = 0x%x, buf[3] = 0x%x, buf[4] = 0x%x, buf[5] = 0x%x, buf[6] = 0x%x\n", 
		buf[0], buf[1], buf[2], buf[3], buf[4], buf[5], buf[6]);

	arr = env->NewIntArray(7);
	env->SetIntArrayRegion(arr, 0, 7, buf);

	ALOGD("%s X", __func__);
	return arr;
}


/*
* return value: 
* a[0]: error code
* a[1]: status , a[2]: votlage , a[3]:current 
* a[4]: temp, a[5]: rotate speed, a[6]: mileage
*/
static jintArray android_server_SerialService_serial_get_control_status(JNIEnv* env, jobject thiz) 
{
	ALOGD("%s E", __func__);
	int ret;
	int buf[7] = {-1};
	jintArray arr;

	if (sSerialDevice) {
		ret = sSerialDevice->serial_control_info(sSerialDevice, TYPE_CONTROL_GET_CONTROL_STATUS,
				0, 0, &buf[1]);
		if (ret != -1)
			buf[0] = 0;
	}else{
		ALOGE("sSerialDevice is null");
	}

	ALOGE("buf[0] = %d, buf[1] = 0x%x, buf[2] = 0x%x, buf[3] = 0x%x, buf[4] = 0x%x, buf[5] = 0x%x, buf[6] = 0x%x\n", 
		buf[0], buf[1], buf[2], buf[3], buf[4], buf[5], buf[6]);

	arr = env->NewIntArray(7);
	env->SetIntArrayRegion(arr, 0, 7, buf);

	ALOGD("%s X", __func__);
	return arr;
}

/*
* return value: 
* array[0]: error code, array[1]: speed limit, array[2]: mode 
*/
static jintArray android_server_SerialService_serial_get_control_speed_config(JNIEnv* env, jobject thiz) 
{
	ALOGD("%s E", __func__);
	int ret;
	int buf[3] = {-1};
	jintArray arr;
//	int *temp;

	if (sSerialDevice) {
		ret = sSerialDevice->serial_control_info(sSerialDevice, TYPE_CONTROL_GET_SPEED_CONFIG, 0, 0, &buf[1]);
		if (ret != -1)
			buf[0] = 0;
	}else{
		ALOGE("sSerialDevice is null");
	}

	ALOGE("buf[0] = %d, buf[1] = 0x%x, buf[2] = 0x%x\n", buf[0], buf[1], buf[2]);

	arr = env->NewIntArray(3);
	env->SetIntArrayRegion(arr, 0, 3, buf);

//	temp = (int *)env->GetIntArrayElements(arr, NULL);
//	ALOGE("temp[0] = 0x%x, temp[1] = 0x%x\n", temp[0], temp[1]);

	ALOGD("%s X", __func__);
	return arr;
}


static JNINativeMethod method_table[] = {
    { "native_open",                "(Ljava/lang/String;)Landroid/os/ParcelFileDescriptor;",
                                    (void*)android_server_SerialService_open },
    {"native_serial_init",     "()Z", (void*)android_server_SerialService_serial_init},

	{"native_serial_set_control_sync_status",   "(II)I", (void*)android_server_SerialService_serial_set_control_sync_status},
    {"native_serial_set_control_speed_config",   "(II)I", (void*)android_server_SerialService_serial_set_control_speed_config},
	{"native_serial_get_control_speed_config",   "()[I", (void*)android_server_SerialService_serial_get_control_speed_config},
	{"native_serial_get_control_status",   "()[I", (void*)android_server_SerialService_serial_get_control_status},
	{"native_serial_get_traffic_status",   "()[I", (void*)android_server_SerialService_serial_get_traffic_status},
    
};

int register_android_server_SerialService(JNIEnv *env)
{
    jclass clazz = env->FindClass("com/android/server/SerialService");
    if (clazz == NULL) {
        ALOGE("Can't find com/android/server/SerialService");
        return -1;
    }

    clazz = env->FindClass("android/os/ParcelFileDescriptor");
    LOG_FATAL_IF(clazz == NULL, "Unable to find class android.os.ParcelFileDescriptor");
    gParcelFileDescriptorOffsets.mClass = (jclass) env->NewGlobalRef(clazz);
    gParcelFileDescriptorOffsets.mConstructor = env->GetMethodID(clazz, "<init>", "(Ljava/io/FileDescriptor;)V");
    LOG_FATAL_IF(gParcelFileDescriptorOffsets.mConstructor == NULL,
                 "Unable to find constructor for android.os.ParcelFileDescriptor");

    return jniRegisterNativeMethods(env, "com/android/server/SerialService",
            method_table, NELEM(method_table));
}

};

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

#define LOG_TAG "SmartBatteryServiceJNI"
#include "utils/Log.h"

#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <hardware/hardware.h>
#include <hardware/smart_battery.h>

namespace android
{
static sbm_device_t *sSbmDevice = NULL;
static sbm_module_t* sSbmModule = NULL;

static inline int sbm_open(const struct hw_module_t* module,
		sbm_device_t** device) {
	ALOGD("%s E", __func__);
	return module->methods->open(module,
			SBM_HARDWARE_MODULE_ID, (struct hw_device_t**)device);
}

static jboolean android_server_sbm_init(JNIEnv *env, jobject thiz)
{
	ALOGD("%s E", __func__);
	sbm_module_t * module = NULL;
	int ret;

	ret = hw_get_module(SBM_HARDWARE_MODULE_ID, (hw_module_t const**)&module);
	if (ret == 0) {
		ALOGD(" get module OK");
		sSbmModule = (sbm_module_t *) module;
		if (sbm_open(&module->common, &sSbmDevice) != 0) 
		{
			ALOGE("sbm_open error");
			return false;
		}
	} else {
		ALOGE("%s : ret = %d, error\n", __func__, ret);
		return false;
	}

	ALOGD("%s X", __func__);
	return true;

}

static jint android_server_sbm_get_battery_temp(JNIEnv* env, jobject thiz) {
	ALOGD("%s E", __func__);
	int ret;
	int result;

	if (sSbmDevice) {
		ret = sSbmDevice->sbm_ioctl(sSbmDevice, TYPE_GET_BATTERY_TEMP, (void *)&result);
	}else{
		ALOGE("sSbmDevice is null");
		return -1;
	}

	ALOGD("%s ret = %d , result = %d X", __func__, ret, result);

	return ((ret < 0) ? -1: result);
}

static jint android_server_sbm_get_battery_voltage(JNIEnv* env, jobject thiz) {
	ALOGD("%s E", __func__);
	int ret;
	int result;

	if (sSbmDevice) {
		ret = sSbmDevice->sbm_ioctl(sSbmDevice, TYPE_GET_BATTERY_VOLTAGE, (void *)&result);
	}else{
		ALOGE("sSbmDevice is null");
		return -1;
	}

	ALOGD("%s ret = %d , result = %d X", __func__, ret, result);

	return ((ret < 0) ? -1: result);
}

static jint android_server_sbm_get_battery_current(JNIEnv* env, jobject thiz) {
	ALOGD("%s E", __func__);
	int ret;
	int result;

	if (sSbmDevice) {
		ret = sSbmDevice->sbm_ioctl(sSbmDevice, TYPE_GET_BATTERY_CURRENT, (void *)&result);
	}else{
		ALOGE("sSbmDevice is null");
		return -1;
	}

	ALOGD("%s ret = %d , result = %d X", __func__, ret, result);

	return ((ret < 0) ? -1: result);
}

static jint android_server_sbm_get_battery_relative_soc(JNIEnv* env, jobject thiz) {
	ALOGD("%s E", __func__);
	int ret;
	int result;

	if (sSbmDevice) {
		ret = sSbmDevice->sbm_ioctl(sSbmDevice, TYPE_GET_REL_SOC, (void *)&result);
	}else{
		ALOGE("sSbmDevice is null");
		return -1;
	}

	ALOGD("%s ret = %d , result = %d X", __func__, ret, result);

	return ((ret < 0) ? -1: result);
}

static jint android_server_sbm_get_battery_absolute_soc(JNIEnv* env, jobject thiz) {
	ALOGD("%s E", __func__);
	int ret;
	int result;

	if (sSbmDevice) {
		ret = sSbmDevice->sbm_ioctl(sSbmDevice, TYPE_GET_ABS_SOC, (void *)&result);
	}else{
		ALOGE("sSbmDevice is null");
		return -1;
	}

	ALOGD("%s ret = %d , result = %d X", __func__, ret, result);

	return ((ret < 0) ? -1: result);
}

static jint android_server_sbm_get_battery_remain_capacity(JNIEnv* env, jobject thiz) {
	ALOGD("%s E", __func__);
	int ret;
	int result;

	if (sSbmDevice) {
		ret = sSbmDevice->sbm_ioctl(sSbmDevice, TYPE_GET_REMAIN_CAPACITY, (void *)&result);
	}else{
		ALOGE("sSbmDevice is null");
		return -1;
	}

	ALOGD("%s ret = %d , result = %d X", __func__, ret, result);

	return ((ret < 0) ? -1: result);
}


static jint android_server_sbm_get_battery_full_capacity(JNIEnv* env, jobject thiz) {
	ALOGD("%s E", __func__);
	int ret;
	int result;

	if (sSbmDevice) {
		ret = sSbmDevice->sbm_ioctl(sSbmDevice, TYPE_GET_FULL_CAPACITY, (void *)&result);
	}else{
		ALOGE("sSbmDevice is null");
		return -1;
	}

	ALOGD("%s ret = %d , result = %d X", __func__, ret, result);

	return ((ret < 0) ? -1: result);
}

static jint android_server_sbm_get_battery_cycle_counts(JNIEnv* env, jobject thiz) {
	ALOGD("%s E", __func__);
	int ret;
	int result;

	if (sSbmDevice) {
		ret = sSbmDevice->sbm_ioctl(sSbmDevice, TYPE_GET_CYCLE_COUNTS, (void *)&result);
	}else{
		ALOGE("sSbmDevice is null");
		return -1;
	}

	ALOGD("%s ret = %d , result = %d X", __func__, ret, result);

	return ((ret < 0) ? -1: result);
}

/*
* return value: 
* array[0]: error code, array[1]: speed limit, array[2]: mode 
*/
#define MAX_SIZE_BUF	14
static jintArray android_server_sbm_get_single_battery_voltage(JNIEnv* env, jobject thiz) 
{
	ALOGD("%s E", __func__);
	int ret;
	int buf[MAX_SIZE_BUF] = {-1};
	jintArray arr;
	int i;

	if (sSbmDevice) {
		ret = sSbmDevice->sbm_ioctl(sSbmDevice, TYPE_GET_SINGLE_BATTERY_VOLTAGE, &buf[1]);
		if (ret != -1)
			buf[0] = 0;
	}else{
		ALOGE("sSbmDevice is null");
	}

	ALOGE("buf #####:");
	for (i = 0; i < MAX_SIZE_BUF; i++) {
		ALOGE("%d ", buf[i]);
	}


	arr = env->NewIntArray(MAX_SIZE_BUF);
	env->SetIntArrayRegion(arr, 0, MAX_SIZE_BUF, buf);

//	temp = (int *)env->GetIntArrayElements(arr, NULL);
//	ALOGE("temp[0] = 0x%x, temp[1] = 0x%x\n", temp[0], temp[1]);

	ALOGD("%s X", __func__);
	return arr;
}


static JNINativeMethod method_table[] = {
    {"nativeInit",     "()Z", (void*)android_server_sbm_init},
	{"nativeGetBatteryTemp",   "()I", (void*)android_server_sbm_get_battery_temp},
	{"nativeGetBatteryVoltage",   "()I", (void*)android_server_sbm_get_battery_voltage},
	{"nativeGetBatteryCurrent",   "()I", (void*)android_server_sbm_get_battery_current},
	{"nativeGetBatteryRelativeSoc",   "()I", (void*)android_server_sbm_get_battery_relative_soc},
	{"nativeGetBatteryAbsoluteSoc",   "()I", (void*)android_server_sbm_get_battery_absolute_soc},
	{"nativeGetBatteryRemainCapactiy",   "()I", (void*)android_server_sbm_get_battery_remain_capacity},
	{"nativeGetBatteryFullCapacity",   "()I", (void*)android_server_sbm_get_battery_full_capacity},
	{"nativeGetBatteryCycleCounts",   "()I", (void*)android_server_sbm_get_battery_cycle_counts},
	{"nativeGetBatterySingleBatteryVoltage",   "()[I", (void*)android_server_sbm_get_single_battery_voltage},
};

int register_android_server_SmartBatteryService(JNIEnv *env)
{
    jclass clazz = env->FindClass("com/android/server/SmartBatteryService");
    if (clazz == NULL) {
        ALOGE("Can't find com/android/server/SmartBatteryService");
        return -1;
    }

    return jniRegisterNativeMethods(env, "com/android/server/SmartBatteryService",
            method_table, NELEM(method_table));
}
};

#include "jni.h"
//#include <utils/Log.h>

#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, fmt, ##args) 

#include<android/log.h>

#define LOG_TAG "webcam"

#include <arpa/inet.h>
#include <netdb.h>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <iostream>
#include <udt.h> 

using namespace std;
 
UDTSOCKET UDTSocket;

UDTSOCKET UDTAudioSocket;

StunSocket *cmdSocket;

StunSocket *audioSocket;

StunSocket *videoSocket;

const int length = 2;

CmdSocket socketArr[length];

char buf[1024];

char* jstringToChar(JNIEnv* env, jstring jstr )
{
	const char *nativeString = env->GetStringUTFChars(jstr, 0); 
	int length = env->GetStringUTFLength(jstr); 
	char * tmp = (char*)malloc(length+1);
	memset(tmp,0,length+1);
	memcpy(tmp, nativeString, length);	
	env->ReleaseStringUTFChars(jstr, nativeString);
	//LOGI("### jstring to char = %s",tmp);
 /*  char* rtn = NULL; 
   jclass clsstring = env->FindClass("java/lang/String"); 
   jstring strencode = env->NewStringUTF("utf-8"); 
   jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B"); 
   jbyteArray barr= (jbyteArray)env->CallObjectMethod(jstr, mid, strencode); 
   jsize alen = env->GetArrayLength(barr); 
   jbyte* ba = env->GetByteArrayElements(barr, JNI_FALSE); 
   if (alen > 0) 
   { 
	 rtn = (char*)malloc(alen + 1); 
	 memcpy(rtn, ba, alen); 
	 rtn[alen] = 0; 
   } 
   env->ReleaseByteArrayElements(barr, ba, 0); */
   return tmp;
   //return rtn;
} 

void clearConnection() {
   //LOGI("### UdtTools start exit!");
   if(cmdSocket>0) {
	if(cmdSocket->type == STUN_SOCK_TCP)
	{
		close(cmdSocket->sock);
	} else {
		UDT::close(cmdSocket->sock);
	}
	cmdSocket->sock = 0;
	cmdSocket = 0;
   }
   if(videoSocket>0) {
	if(videoSocket->type == STUN_SOCK_TCP)
	{
		close(videoSocket->sock);
	} else {
		UDT::close(videoSocket->sock);
	}
	videoSocket->sock = 0;
	videoSocket = 0;
      
   }
   if(audioSocket>0) {
	if(audioSocket->type == STUN_SOCK_TCP)
	{
		close(audioSocket->sock);
	} else {
		UDT::close(audioSocket->sock);
	}
	audioSocket->sock = 0;
	audioSocket = 0;
      
   }  
   LOGI("### UdtTools clearConnection over!");
}

// release connected socket
extern "C" void JNICALL Java_com_iped_ipcam_gui_UdtTools_release(JNIEnv *env, jobject thiz)
{
   if(UDTSocket>0) {
      UDT::close(UDTSocket);
   }
}

/****************** new udt socket ********************/

int fetchCamIndex;

StunSocket * checkCmdSocketValid(char *id) {
	StunSocket *cmdSocketTmp;
	for(int i=0;i<length;i++) {
		cmdSocketTmp = socketArr[i].cmdSocket;
		char *tmp = socketArr[i].id;
		if(tmp != 0 && strcmp( tmp, id )==0 && cmdSocketTmp != NULL && cmdSocketTmp->sock >0){	
		  	return cmdSocketTmp; 
		}
	}
	return 0;
}

extern "C" int JNICALL Java_com_iped_ipcam_gui_UdtTools_startSearch(JNIEnv *env, jobject thiz)
{	
	LOGI("### UdtTools startSearch");
	fetchCamIndex = 0;
	monitor_search_lan(NULL, NULL, NULL);
	LOGI("### UdtTools search over!");
        return 0;
}

extern "C" jstring JNICALL Java_com_iped_ipcam_gui_UdtTools_fetchCamId(JNIEnv *env, jobject thiz)
{
	jstring camIdTmp;
  	if (cam_lan_id[fetchCamIndex][0] != '\0' && fetchCamIndex <64)
        {
		camIdTmp = env->NewStringUTF(cam_lan_id[fetchCamIndex]); 
		fetchCamIndex++;
		LOGI("## UdtTools_fetch index = %d",fetchCamIndex);
		return camIdTmp;  
	}
	return NULL;
}

extern "C" void JNICALL Java_com_iped_ipcam_gui_UdtTools_stopSearch(JNIEnv *env, jobject thiz)
{	
	LOGI("### UdtTools stopSearch");	
	fetchCamIndex = 0;
	monitor_search_stop();
	LOGI("### UdtTools stopSearch over!");

}


extern "C" jint JNICALL Java_com_iped_ipcam_gui_UdtTools_monitorSocket(JNIEnv *env, jobject thiz, jstring camId)
{
	clearConnection();
	LOGI("### start monitorSocke");
	monitor_local_addr();
	//LOGI("### end monitor_local_addr");
	char* id = jstringToChar(env,camId);
	cmdSocket = monitor_socket(id, STUN_SOCK_AUTO);
	if(cmdSocket==NULL || cmdSocket->sock < 0) {
		free(id);
		LOGI("### UdtTools monitor cmd socket fail !  error info = %i", cmdSocket->sock);
		return cmdSocket->sock;
	}
	//LOGI("### cmdSocket socket = %d", cmdSocket->sock);
	audioSocket = monitor_socket(id, STUN_SOCK_AUTO);
	if(audioSocket == NULL || audioSocket->sock < 0) {
		free(id);
		LOGI("### UdtTools monitor audio socket fail!  error info = %i", audioSocket->sock);
		return cmdSocket->sock;
	}
	//LOGI("### audioSocket socket = %d", audioSocket->sock);
	videoSocket = monitor_socket(id, STUN_SOCK_AUTO);
	if(videoSocket == NULL || videoSocket->sock < 0) {
		free(id);
		LOGI("### UdtTools monitor video socket fail!  error info = %i", videoSocket->sock);
		return cmdSocket->sock;
	}
	//LOGI("### videoSocket socket = %d", videoSocket->sock);
	LOGI("### UdtTools monitorSocket success!");
	free(id);
	return 1;
}

extern "C" jint JNICALL Java_com_iped_ipcam_gui_UdtTools_initialSocket(JNIEnv *env, jobject thiz, jstring camId, jstring rand)
{
	LOGI("### UdtTools start initialSocket!");	
	char * random = jstringToChar(env,rand);
	char * id = jstringToChar(env,camId);
	int res = -1;
	int randomLength = 10;
	*(random + 8)='C';
	*(random + 9)=0;
	
	if(cmdSocket->type == STUN_SOCK_TCP) {
		res = stun_tcp_sendn(cmdSocket->sock,random,randomLength);
	} else {
		res = stun_udt_sendn(cmdSocket->sock,random,randomLength);
	}
	if(res <=-1) {
		free(random);
		free(id);
		LOGI("### UdtTools start initial cmd socket fail !  error info = %i", cmdSocket->sock);
		return res;
	}
	*(random + 8)='A';
	if(audioSocket->type == STUN_SOCK_TCP) {
		res = stun_tcp_sendn(audioSocket->sock,random,randomLength);
	} else {
		res = stun_udt_sendn(audioSocket->sock,random,randomLength);
	}
	if(res <=-1) {
		free(random);
		free(id);
		LOGI("### UdtTools start initial audio socket fail !  error info = %i", audioSocket->sock);
		return res;
	}
	*(random + 8)='V';
	if(videoSocket->lan == 1) {//in
		*(random + 9)=1;
	} else { // out
		*(random + 9)=0;
	}
	if(videoSocket->type == STUN_SOCK_TCP) {
		res = stun_tcp_sendn(videoSocket->sock,random,randomLength);
	} else {
		res = stun_udt_sendn(videoSocket->sock,random,randomLength);
	}	
	if(res <=-1) {
		free(random);
		free(id);
		LOGI("### UdtTools start initial video socket fail!  error info = %i", videoSocket->sock);
		return res;
	}
	socketArr[0].id = id;
	socketArr[0].cmdSocket = cmdSocket;
	LOGI("### UdtTools initSocket success!");
	free(random);
	return res;
}



extern "C" jint JNICALL Java_com_iped_ipcam_gui_UdtTools_monitorCmdSocket(JNIEnv *env, jobject thiz, jstring camId,jstring rand)
{
	LOGI("### start monitor Cmd Socket");
	monitor_local_addr();
	//LOGI("### end monitor_local_addr");
	char* id = jstringToChar(env,camId);
	StunSocket *configSocket = monitor_socket(id, STUN_SOCK_AUTO);
	//LOGI("### monitorCmdSocket Socket socket = %p", configSocket->sock);
	if(configSocket == NULL || configSocket->sock < 0) {
		free(id);
		LOGI("### UdtTools monitor cmd socket fail !  error info = %i", configSocket->sock);
		return configSocket->sock;
	}
	LOGI("### UdtTools monitor cmd socket success !");
	char * random = jstringToChar(env,rand);
	int randomLength = 10;
	*(random + 8)='C';
	*(random + 9)=0;
	int res = -1;	
	if(configSocket->type == STUN_SOCK_TCP) {
		res = stun_tcp_sendn(configSocket->sock,random,randomLength);
	} else {
		res = stun_udt_sendn(configSocket->sock,random,randomLength);
	}

	if(res <=-1) {
		free(id);
		free(random);
		LOGI("### UdtTools init cmd socket fail !  error info = %i", configSocket->sock);
		return res;
	}
	socketArr[1].id = id;
	socketArr[1].cmdSocket = configSocket;
	LOGI("### UdtTools monitor cmd socket success !");
	free(random);
	return res;
}

extern "C" jint JNICALL Java_com_iped_ipcam_gui_UdtTools_sendCmdMsgById(JNIEnv *env, jobject thiz, jstring camId, jstring cmdName, jint cmdNameLength)
{	
	char* id = jstringToChar(env,camId);
	StunSocket *cmdSendSocket;
	cmdSendSocket = checkCmdSocketValid(id);
	if(cmdSendSocket <= 0) {
		free(id);
		LOGI("### UdtTools send cmd socket is invalid!");
		return cmdSendSocket->sock;
	}
	char * cmd = jstringToChar(env,cmdName);
	int res = 0;
	if(cmdSendSocket->type == STUN_SOCK_TCP) {
		res =  stun_tcp_sendmsg(cmdSendSocket->sock, cmd, cmdNameLength);
	} else {
		res = stun_sendmsg(cmdSendSocket->sock, cmd, cmdNameLength);
	}
	LOGI("### UdtTools send cmd name %s, send msg length %d", cmd,res);
	free(id);
	free(cmd);
	return res;
}


extern "C" jint JNICALL Java_com_iped_ipcam_gui_UdtTools_recvCmdMsgById(JNIEnv *env, jobject thiz, jstring camId, jbyteArray buffer, jint bufferLength)
{
	LOGI("### UdtTools start recv cmd msg!");
	char* id = jstringToChar(env,camId);
	StunSocket *cmdSendSocket;
	cmdSendSocket = checkCmdSocketValid(id);
	if(cmdSendSocket <= 0) {
		free(id);
		LOGI("### UdtTools recv cmd socket is invalid!  error info = %i", cmdSendSocket->sock);
		return cmdSendSocket->sock;
	}
	char * tmp = (char*)malloc(bufferLength); 
	LOGI("### UdtTools wait for recv cmd ...");
	int dataLength = 0;
	if(cmdSendSocket->type == STUN_SOCK_TCP) {
		dataLength = stun_tcp_recvmsg(cmdSendSocket->sock,tmp,bufferLength,NULL, NULL);
	} else {
		dataLength = stun_recvmsg(cmdSendSocket->sock,tmp,bufferLength,NULL, NULL);
	}
	
	if(dataLength <= 0) {
	     free(id);
	     free(tmp);
	     LOGI("### UdtTools recv error!");
	     return cmdSendSocket->sock;
	}
	LOGI("### UdtTools recv cmd success!");
	env->SetByteArrayRegion(buffer, 0, dataLength,(jbyte*) tmp);  
	free(id);
	free(tmp);
	return dataLength;	
}

char recvAudioBuf[1024];

extern "C" jint JNICALL Java_com_iped_ipcam_gui_UdtTools_recvAudioMsg(JNIEnv *env, jobject thiz,jint smallAudioBufferLength, jbyteArray buffer, jint bigAudioBufferLength)
{
	int pos=0;
	int dataLength;
	//char recvAudioBuf[smallAudioBufferLength];
	if(audioSocket->type == STUN_SOCK_TCP) 
	{
		dataLength = recv(audioSocket->sock, recvAudioBuf, smallAudioBufferLength,0);
	} else {
		dataLength = UDT::recv(audioSocket->sock, recvAudioBuf, smallAudioBufferLength,0);
	}
	if(dataLength <= 0) { 
	    LOGI("UdtTools recvAudioMsg over");
	    return pos = -1;
	}
	//LOGI("UdtTools recv Audio Msg length = %d,%d,%d", dataLength,recvAudioBuf[1],recvAudioBuf[2]);
	//memcpy(audioBuf+pos,recvAudioBuf,dataLength);
	//pos+=dataLength;

        env->SetByteArrayRegion(buffer, 0, dataLength,(jbyte*) recvAudioBuf);  
       return dataLength;	
}

extern "C" jint JNICALL Java_com_iped_ipcam_gui_UdtTools_sendAudioMsg(JNIEnv *env, jobject thiz,jbyteArray amrBuffer, jint amrBufferLength)
{
	char* data = (char*)malloc(amrBufferLength);
	env->GetByteArrayRegion (amrBuffer, (jint)0, (jint)amrBufferLength, (jbyte*)data);
	int dataLength;
	//char recvAudioBuf[smallAudioBufferLength];
	if(audioSocket->type == STUN_SOCK_TCP) 
	{
		dataLength = send(audioSocket->sock, data, amrBufferLength, 0);
	} else {
		dataLength = UDT::send(audioSocket->sock, data, amrBufferLength, 0);
	}
	//LOGI("UdtTools send audio Msg length = %d", dataLength);
	free(data);  
        return dataLength;	
}

extern "C" jint JNICALL Java_com_iped_ipcam_gui_UdtTools_recvVideoMsg(JNIEnv *env, jobject thiz,jbyteArray buffer, int bufferLength)
{
    int dataLength;
    if(videoSocket->type == STUN_SOCK_TCP) 
    {
	dataLength = recv(videoSocket->sock,buf, 1024, 0);
    } else {
	dataLength = UDT::recv(videoSocket->sock,buf,1024,0);
    }
    
    //LOGI("### UdtTools recvVideoMsg result %d", dataLength);
    if(dataLength <= 0) {
	LOGI("UdtTools recvVideoMsg over");
     	return videoSocket->sock;
    }
    env->SetByteArrayRegion(buffer, 0, dataLength,(jbyte*) buf);  
    //LOGI("### UdtTools recvVideoMsg result aaaaaaa %d", dataLength);
    return dataLength;
}

extern "C" jint JNICALL Java_com_iped_ipcam_gui_UdtTools_sendPTZMsg(JNIEnv *env, jobject thiz,jbyteArray buffer)
{
	jint len = env->GetArrayLength(buffer);
	char* data = (char*)malloc(len);
	env->GetByteArrayRegion (buffer, (jint)0, (jint)len, (jbyte*)data);
	int res = 0;
	if(cmdSocket->type == STUN_SOCK_TCP) {
		res =  stun_tcp_sendmsg(cmdSocket->sock, data, len);
	} else {
		res = stun_sendmsg(cmdSocket->sock, data, len);
	}
	free(data);
	LOGI("### UdtTools send PTZ msg over!");
	return res;
}

extern "C" void JNICALL Java_com_iped_ipcam_gui_UdtTools_close(JNIEnv *env, jobject thiz)
{
	if(socketArr[1].cmdSocket > 0){
		if(socketArr[1].cmdSocket->lan == STUN_SOCK_TCP){
			close(socketArr[1].cmdSocket->sock);
		} else {
			UDT::close(socketArr[1].cmdSocket->sock);
		}
		socketArr[1].id = 0;
		socketArr[1].cmdSocket = 0;
		LOGI("### UdtTools release cmd socket!");
	}
}

extern "C" void JNICALL Java_com_iped_ipcam_gui_UdtTools_exit(JNIEnv *env, jobject thiz)
{
   clearConnection();
}

extern "C" void JNICALL Java_com_iped_ipcam_gui_UdtTools_startUp(JNIEnv *env, jobject thiz)
{
	UDT::startup();
	LOGI("### UdtTools start up!");
}

extern "C" void JNICALL Java_com_iped_ipcam_gui_UdtTools_cleanUp(JNIEnv *env, jobject thiz)
{
	 UDT::cleanup();
	 LOGI("### UdtTools clean up!");
}


extern "C" jint JNICALL Java_com_iped_ipcam_gui_UdtTools_checkCmdSocketEnable(JNIEnv *env, jobject thiz, jstring camId)
{
	char* id = jstringToChar(env,camId);//length
	StunSocket *cmd = checkCmdSocketValid(id);
	if(cmd == NULL || cmd->sock<0) {
		free(id);
		return -1;
	}
	free(id);
	return 1;
}



/*
 * Copyright (C) 2008 The Android Open Source Project
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

#define LOG_TAG "MyJNIXPDF"
#include <utils/Log.h>

#include <stdio.h>

#include "jni.h"


// add xpdf c headers
#include "aconf.h"
#include <stdlib.h>
#include "GString.h"
#include "GlobalParams.h"
#include "Object.h"
#include "PDFDoc.h"
#include "SplashBitmap.h"
#include "Splash.h"
#include "SplashOutputDev.h"
#include "config.h"
#include "parseargs.h"
#include "parseargs.c"
//#include <iconv.h>
#include <string.h>



static int firstPage = 1;
static int lastPage = 0;
static int resolution = 150;
static int realwidth = 0;
static int realheigt = 0;
//#if HAVE_CONSTRAINT_PAGESIZE//comment by byron
static int displayWidth = 600;
static int displayHeight = 800;
//#endif
#if HAVE_TOTALPAGE//comment by byron
static GBool page = gFalse;
#endif
static GBool mono = gFalse;
static GBool gray = gFalse;
static char enableT1libStr[16] = "";
static char enableFreeTypeStr[16] = "";
static char antialiasStr[16] = "";
static char ownerPassword[33] = "";
static char userPassword[33] = "";
static GBool quiet = gFalse;
static char cfgFileName[256] = "";
static GBool printVersion = gFalse;
static GBool printHelp = gFalse;

static PDFDoc *doc;
#define BUFLEN 256 


SplashOutputDev *splashOut;


static ArgDesc argDesc[] = {
  {"-f",      argInt,      &firstPage,     0,
   "first page to print"},
  {"-l",      argInt,      &lastPage,      0,
   "last page to print"},
#if HAVE_CONSTRAINT_PAGESIZE//comment by byron
  {"-w",      argInt,      &displayWidth,    0,
   "displayWidth, in pixels (default is 600)"},
  {"-h",      argInt,      &displayHeight,    0,
   "displayHeight, in pixels (default is 800)"},
#else
  {"-r",      argInt,      &resolution,    0,
   "resolution, in DPI (default is 150)"},
#endif
#if HAVE_TOTALPAGE//comment by byron
  {"-p",      argFlag,     &page,          0,
   "get the PDF file total pages"},
#endif
  {"-mono",   argFlag,     &mono,          0,
   "generate a monochrome PBM file"},
  {"-gray",   argFlag,     &gray,          0,
   "generate a grayscale PGM file"},
#if HAVE_T1LIB_H
  {"-t1lib",      argString,      enableT1libStr, sizeof(enableT1libStr),
   "enable t1lib font rasterizer: yes, no"},
#endif
#if HAVE_FREETYPE_FREETYPE_H | HAVE_FREETYPE_H
  {"-freetype",   argString,      enableFreeTypeStr, sizeof(enableFreeTypeStr),
   "enable FreeType font rasterizer: yes, no"},
#endif
  {"-aa",         argString,      antialiasStr,   sizeof(antialiasStr),
   "enable font anti-aliasing: yes, no"},
  {"-opw",    argString,   ownerPassword,  sizeof(ownerPassword),
   "owner password (for encrypted files)"},
  {"-upw",    argString,   userPassword,   sizeof(userPassword),
   "user password (for encrypted files)"},
  {"-q",      argFlag,     &quiet,         0,
   "don't print any messages or errors"},
  {"-cfg",        argString,      cfgFileName,    sizeof(cfgFileName),
   "configuration file to use in place of .xpdfrc"},
  {"-v",      argFlag,     &printVersion,  0,
   "print copyright and version info"},
  {"-h",      argFlag,     &printHelp,     0,
   "print usage information"},
  {"-help",   argFlag,     &printHelp,     0,
   "print usage information"},
  {"--help",  argFlag,     &printHelp,     0,
   "print usage information"},
  {"-?",      argFlag,     &printHelp,     0,
   "print usage information"},
  {NULL}
};

//com.iped.xpdf
extern "C" JNIEXPORT jint JNICALL Java_com_iped_xpdf_gui_XpdfBitmapView_getPdfWidth(JNIEnv *env, jobject thiz) {
    return realwidth;
}

//com.iped.xpdf
extern "C" JNIEXPORT jint JNICALL Java_com_iped_xpdf_gui_XpdfBitmapView_getPdfHeight(JNIEnv *env, jobject thiz) {
    return realheigt;
}

char* jstringTostring(JNIEnv* env, jstring jstr)   
{           
	 char* rtn = NULL;   
	 jclass clsstring = env->FindClass("java/lang/String");   
	 jstring strencode = env->NewStringUTF("UTF-8");   
	 jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");   
	 jbyteArray barr= (jbyteArray)env->CallObjectMethod(jstr, mid, strencode);   
	 jsize alen = env->GetArrayLength(barr);   
	 LOGI("buffer length = '%d'", alen);
	 jbyte* ba = env->GetByteArrayElements(barr, JNI_FALSE);   
	  if (alen > 0)   
	  {   
	    rtn = (char*)malloc(alen + 1);   
	    memcpy(rtn, ba, alen);   
	    rtn[alen] = 0;   
	  }   
	  env->ReleaseByteArrayElements(barr, ba, 0);   
	  return rtn;   
}  


char*  jstringToPchar(JNIEnv* env, jstring jstr )
{
	int length = (env)->GetStringUTFLength(jstr);
	//LOGI("length length length = '%d'", length);
    const char *strDest;
	
	strDest = env->GetStringUTFChars(jstr,JNI_FALSE);
	LOGI("open pdf file name = '%s'", strDest);
	if (strDest == NULL)
	{
		return NULL; //这里注意可能因为内存不足，需要抛出OutOfMemoryError异常，所以先返回空，有关JNI的异常处理Android开发网将在下面的文章中详细讲解
    }
	char* rtn = (char*)malloc(length);
	memcpy(rtn,strDest,length);
	env->ReleaseStringUTFChars(jstr, strDest); //strDest用完了要释放内存
    return rtn;
}

// in use this function
extern "C" JNIEXPORT jint JNICALL Java_com_iped_xpdf_gui_XpdfBitmapView_openXpdf(JNIEnv *env, jobject obj, jstring filename, jstring gray2) {
	int length = (env)->GetStringUTFLength(filename);
	GString *fileName;
	GString *ownerPW, *userPW;
	SplashColor paperColor;
    //ch =  jstringTostring(env, filename);
   char * ch = jstringToPchar(env, filename);
	//LOGI("open pdf file name = '%s'", ch);
    fileName = new GString(ch, length);
	globalParams = new GlobalParams(cfgFileName);
	//LOGI("cfgFileName='%s'", cfgFileName);
	globalParams->setupBaseFonts(NULL);
	if (enableT1libStr[0]) {
		if (!globalParams->setEnableT1lib(enableT1libStr)) {
		  fprintf(stderr, "Bad '-t1lib' value on command line\n");
		}
	  }
	  if (enableFreeTypeStr[0]) {
		if (!globalParams->setEnableFreeType(enableFreeTypeStr)) {
		  fprintf(stderr, "Bad '-freetype' value on command line\n");
		}
	  }
	  if (antialiasStr[0]) {
		if (!globalParams->setAntialias(antialiasStr)) {
		  fprintf(stderr, "Bad '-aa' value on command line\n");
		}
	  }
	  if (quiet) {
		return -1;
	  }
	  
	  // open PDF file
	  if (ownerPassword[0]) {
	    ownerPW = new GString(ownerPassword);
	  } else {
	    ownerPW = NULL;
	  }
	  if (userPassword[0]) {
	    userPW = new GString(userPassword);
	  } else {
	    userPW = NULL;
	  }

	  doc = new PDFDoc(fileName, ownerPW, userPW);
	  if (userPW) {
	    delete userPW;
	  }
	  if (ownerPW) {
	    delete ownerPW;
	  }
	  LOGI("doc is OK1='%d'", doc->isOk());
	  if (!doc->isOk()) {
	    return -2;
	  }
	  gray = gTrue;
	  if (mono) {
	    paperColor[0] = 1;
	    splashOut = new SplashOutputDev(splashModeMono1, 1, gFalse, paperColor);
	  } else if (gray) {  
		 paperColor[0] = 0xff;
	    splashOut = new SplashOutputDev(splashModeMono8, 1, gFalse, paperColor);
	  } else {
	      paperColor[0] = paperColor[1] = paperColor[2] = 0xff;
	    splashOut = new SplashOutputDev(splashModeRGB8, 1, gFalse, paperColor);
	}  
  	splashOut->startDoc(doc->getXRef());
	LOGI("doc open success, doc page numbers = '%d'", doc->getNumPages());
	return doc->getNumPages();
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_com_iped_xpdf_gui_XpdfBitmapView_getPNMBuffer(JNIEnv *env, jobject obj, jint pg, jint w, jint h) {
	char ppmFile[512];

	SplashBitmap * bitMap;
    jbyteArray a1;
    displayWidth = w;
    displayHeight = h;
    double pageWidth;
    double pageHeight;
    double kx;
    double ky;
    // get PPM data
    pageWidth = doc->getPageCropWidth(pg);
    kx = ((double)displayWidth) / pageWidth;
	kx *= 72.0;
	pageHeight = doc->getPageCropHeight(pg);
	ky = ((double)displayHeight) / pageHeight;
	ky *= 72.0;
	resolution = (int)kx > (int)ky ? (int)ky : (int)kx;
	doc->displayPage(splashOut, pg, resolution, resolution, 0, gFalse, gTrue, gFalse);
	 sprintf(ppmFile, "%.*s-%06d.%s",(int)sizeof(ppmFile) - 32, "/sdcard/temp/", pg,  mono ? "pbm" : gray ? "pgm" : "ppm");
	
	splashOut->getBitmap()->writePNMFile(ppmFile);
	bitMap = splashOut->getBitmap();
	unsigned char * tempArr = bitMap->getPNMByteBuffer();
	realwidth = bitMap->getWidth();
	realheigt = bitMap->getHeight();
	jbyte * ji = (jbyte*)tempArr;
	int bufferLength = bitMap->getPNMBufferLength();
	a1 = env->NewByteArray(bufferLength);
	env->SetByteArrayRegion(a1,0,bufferLength, ji);
	delete []tempArr;
    return a1;
}





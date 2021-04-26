#include "Native.h"
// #include <StubPreamble.h>
#include <stdio.h>


JNIEXPORT jint JNICALL Java_Native_nativeFunc
  (JNIEnv * env , jobject b, jobject c, jobject d) {
      jclass nodeClass = env->FindClass("Node");
      
      jfieldID num = env->GetFieldID(nodeClass, "a", "I");
      jint cnum = env->GetIntField(c, num);
      jint dnum = env->GetIntField(d, num);
      return cnum + dnum;
  }

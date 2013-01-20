//
//  main.c
//  apple
//
//  Created by Wilbur on 3/7/12.
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//


#include "api/kxplayer/agent.h"

#define MP3_FILE    "/Users/kaixin/Work/RadioPlayer/KaixinPlayer/xiarixing.wma"
#define MMSH_URL    "mmsh://202.177.192.111/radio2?MSWMExt=.asf"
#define HTTP_MMSH         "http://202.177.192.111/radio2"
#define HTTP_WMA    "http://www.liming2009.com/108/music/ruguonishiwodechuanshuo.wma"

#define PLAY_URI    HTTP_MMSH

#include <osapi.h>
#include <osapi/log.h>
#include <osapi/thread.h>
#include <osapi/system.h>

#include "api/kxplayer/player.h"

static int start(struct kxplayer_avcontext* context, void* userdata) {
    OS_LOG(os_log_debug, "start callback.");
    return 0;
}

static void receive(void* data, size_t size, void* userdata) {
    OS_LOG(os_log_debug, "receive data.");
}

int main(int argc, char *argv[])
{
    if (kxplayer_initialize() != 0) {
        OS_LOG(os_log_error, "unable to initialize kxplayer.");
        return -1;
    }
    os_log_setlevel(os_log_trace);
    struct os_log_backend* console = os_log_backend_create(os_log_console_interface(), NULL);
    os_log_add_backend(console);
    
    struct kxplayer_agent_option option;
    option.start_callback = start;
    option.receive_callback = receive;
    option.finish_callback = NULL;
    option.userdata = NULL;
    struct kxplayer_agent* agent = kxplayer_agent_create(&option);
    if (agent == NULL) {
        OS_LOG(os_log_error, "unable to create agent.");
        return -1;
    }
    
    if (kxplayer_agent_open(agent, PLAY_URI) != 0) {
        OS_LOG(os_log_error, "unable to open uri %s.", PLAY_URI);
        return -1;
    }
    
    os_sleep(5000);
    kxplayer_agent_open(agent, MP3_FILE);
    os_sleep(999999);
    
    kxplayer_agent_stop(agent);
    
    kxplayer_terminate();
    
    return 0;
}














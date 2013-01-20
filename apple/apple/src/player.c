
#include <osapi.h>
#include <osapi/thread.h>
#include <osapi/system.h>
#include <libavformat/avformat.h>

#include "api/kxplayer/player.h"
#include "api/kxplayer/agent.h"

int kxplayer_createplayer(void);
void kxplayer_releaseplayer(void);

int kxplayer_initialize(void) {
    if (osapi_initialize() != 0) {
        fprintf(stderr, "failed to initialize osapi.\n");
        return -1;
    }
    
    av_log_set_level(AV_LOG_DEBUG);
    av_register_all();
    avformat_network_init();
    
    return 0;
    
err1:
    osapi_terminate();
    return -1;
}

void kxplayer_terminate(void) {
    avformat_network_deinit();
    osapi_terminate();
}









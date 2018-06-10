if(window.vid && (vid.indexOf('.mp4') > 0 || vid.indexOf('.m3u8') > 0)) {
    console.log('type=1;' + vid);
    window.bridge.loadVideo(vid);
} else if (window.ckplayerLoad) {
    window.ckplayerLoad = function(data) {
            console.log('type=2;' + data.url);
            window.bridge.loadVideo(data.url);
        };
} else if (window.player) {
    var _send = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.send = function() {
        var _onload = this.onload;
        this.onload = function() {
            console.log(this.responseText);
            var data = JSON.parse(this.responseText);
            if (data.ext && (data.ext == 'xml' || data.ext == 'xml_client')) {
                console.log("type=5")
                window.bridge.intercept();
            } else if (data.url) {
                console.log('type=3;' + data.url);
                window.bridge.loadVideo(data.url);
            }
        }
        return _send.apply(this, arguments);
    }
    player();
} else if (window.vid) {
    console.log('type=4;' + vid);
    window.bridge.loadVideo(vid);
} else {
    var video = document.getElementsByTagName('video');
    if(video.length > 0)
        window.bridge.loadVideo(video[0].getAttribute('src'));
    else
        window.bridge.destroy();
}
var _open = XMLHttpRequest.prototype.open;
XMLHttpRequest.prototype.open = function() {
    if ('GET' == arguments[0]) {
        var prefix = arguments[1].indexOf('?') > 0? '&' : '?';
        arguments[1] = arguments[1] + prefix + "package=com.zhenl.crawler";
    }
    return _open.apply(this, arguments);
}
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
            } else {
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
    window.bridge.destroy();
}
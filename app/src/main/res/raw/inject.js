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
            var data = JSON.parse(this.responseText);
            console.log('type=3;' + data.url);
            window.bridge.loadVideo(data.url);
        }
        return _send.apply(this, arguments);
    }
    player();
} else {
    window.bridge.destroy();
}
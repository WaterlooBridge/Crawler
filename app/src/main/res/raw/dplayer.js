function DPlayer(data) {
    console.log(JSON.stringify(data));
    if (data.video && data.video.url) {
        window.bridge.loadVideo(window.bridge.makeAbsoluteUrl(data.video.url));
    }
}

function Aliplayer(data) {
    console.log(JSON.stringify(data));
    if (data.source) {
        window.bridge.loadVideo(window.bridge.makeAbsoluteUrl(data.source));
    }
}

window.bridge.makeAbsoluteUrl = function(url) {
    if (url.startsWith('//'))
        url = window.location.protocol + url;
    else if (url.startsWith('/'))
        url = window.location.protocol + '//' + window.location.host + url;
    return url;
}

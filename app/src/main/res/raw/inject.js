var mplayer = document.getElementsByClassName("mplayer");
if (mplayer.length > 0)
    var iframe = mplayer[0].getElementsByTagName("iframe");

if (iframe && iframe.length > 0) {
    window.bridge.loadUrl(iframe[0].getAttribute("src"));
} else if (window.player && player._options && player._options.source) {
    window.bridge.loadVideo(player._options.source);
} else {
    intercept();
    scanFrame();
}

function intercept() {
    var observer = new window.MutationObserver(function (mutations) {
        scanPage();
    });

    observer.observe(document, {
        childList: true, // listen to changes to node children
        subtree: true // listen to changes to descendants as well
    });

    scanPage();
}

function scanPage() {
    var allUrlsList = [];

    var a = document.getElementsByTagName('video');
    for (var i = 0; i < a.length; i++) {
        var link = a[i];
        var u = false;
        if (link.src)
            u = link.src;
        else if (link.hasAttribute('src'))
            u = link.getAttribute('src');
        if (u && u.startsWith('http'))
            allUrlsList.push({'url': u});
    }

    console.log(JSON.stringify(allUrlsList));
    if (allUrlsList.length > 0)
        window.bridge.loadVideo(allUrlsList[0].url);
}

function scanFrame() {
    var iframe = document.getElementsByTagName('iframe');
    for (var i = 0; i < iframe.length; i++) {
        if (iframe[i].id == 'player_swf') {
            window.bridge.loadUrl(iframe[i].getAttribute("src"));
            break;
        } else if (i == iframe.length - 1) {
            window.bridge.loadUrl(iframe[0].getAttribute("src"));
        }
    }
}

setTimeout(function(){
    window.bridge.destroy();
}, 5000);
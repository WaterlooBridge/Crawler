function scanPage()
{
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

    var dp = document.getElementById('dplayer');
    if (dp && window.onload) {
        window.onload();
    } else if (window.qs && qs.url) {
        window.bridge.loadVideo(window.bridge.makeAbsoluteUrl(qs.url));
    } else if (window.lele && lele.start) {
        lele.start();
    } else if (window.player && player.getSourceUrl) {
        window.bridge.loadVideo(player.getSourceUrl());
    }
}

function DPlayer(data) {
    console.log(JSON.stringify(data));
    if (data.video && data.video.url) {
        window.bridge.loadVideo(window.bridge.makeAbsoluteUrl(data.video.url));
    }
}

function leleplayer(config) {
    console.log(config.video.url);
    window.bridge.loadVideo(config.video.url);
}

window.bridge.urlParam = function(name) {
  var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
  if(!results || results.length <= 1) {
    return "";
  }
  return results[1] || 0;
}

window.bridge.makeAbsoluteUrl = function(url) {
    if (url.startsWith('//'))
        url = window.location.protocol + url;
    else if (url.startsWith('/'))
        url = window.location.protocol + '//' + window.location.host + url;
    return url;
}

var scan = function() {
    scanFrame(document);
    scanPage();
}

setInterval(scan, 1000);

function scanFrame(document) {
    var iframe = document.getElementsByTagName('iframe');
    for (var i = 0; i < iframe.length; i++) {
        var src = iframe[i].getAttribute("src");
        src = window.bridge.makeAbsoluteUrl(src);
        console.log("iframe src=" + src);
        if (iframe[i].id == 'player_swf') {
            window.bridge.loadUrl(src);
            break;
        } else if (iframe[i].id == 'age_playfram') {
            window.bridge.loadUrl(src);
            break;
        } else if (iframe[i].id == 'playiframe') {
            window.bridge.loadUrl(src);
            break;
        }
    }
}

setTimeout(function(){
    window.bridge.destroy();
}, 5000);
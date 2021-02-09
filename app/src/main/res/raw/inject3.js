function scanPage()
{
    var allUrlsList = [];

    var a = document.getElementsByTagName('video');
    for (var i = 0; i < a.length; i++)
    {
        var link = a[i];
        var u = false;
        if (link.src)
            u = link.src;
        if (!u && link.hasAttribute('data-thumb'))
        {
            u = myTrim(link.getAttribute('data-thumb'));
            if (u.indexOf("http") == -1)
                u = "http:" + u;
        }
        if ( u)
        {
            var title = '';
            if (link.hasAttribute('alt'))
                title = myTrim(link.getAttribute('alt'));
            else if (link.hasAttribute('title'))
                title = myTrim(link.getAttribute('title'));
            if (!title)
                title=document.title;
            var cl = "";
            if (link.hasAttribute('class'))
                cl = myTrim(link.getAttribute('class'));

            allUrlsList.push({'url': u,'title': title});
        }
    }

    console.log(JSON.stringify(allUrlsList));
    if (allUrlsList.length > 0)
        window.bridge.loadVideo(allUrlsList[0].url);

    var dp = document.getElementById('dplayer');
    if (dp && window.onload) {
        window.onload();
    } else if (window.qs && qs.url) {
        window.bridge.loadVideo(window.bridge.makeAbsoluteUrl(qs.url));
    }
}

function DPlayer(data) {
    console.log(JSON.stringify(data));
    if (data.video && data.video.url) {
        window.bridge.loadVideo(data.video.url);
    }
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
        console.log("iframe src=" + src);
        if (window.now) {
            if (src.indexOf(now) != -1)
                window.bridge.loadUrl(src);
            else
                scanFrame(iframe[i].contentWindow.document);
        } else if (iframe[i].id == 'player_swf') {
            window.bridge.loadUrl(src);
            break;
        } else if (iframe[i].id == 'age_playfram') {
            src = window.bridge.makeAbsoluteUrl(src);
            window.bridge.loadUrl(src);
            break;
        } else if (i == iframe.length - 1) {
            window.bridge.loadUrl(iframe[0].getAttribute("src"));
        }
    }
}

setTimeout(function(){
    window.bridge.destroy();
}, 5000);
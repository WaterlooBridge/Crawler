if (window.vid && (vid.indexOf('.mp4') > 0 || vid.indexOf('.m3u8') > 0)) {
    console.log('type=1;' + vid);
    window.bridge.loadVideo(vid);
} else if (window.ckplayerLoad) {
    window.ckplayerLoad = function(data) {
            console.log('type=2;' + data.url);
            window.bridge.loadVideo(data.url);
        };
} else if (window.player) {
    if (player instanceof Function) {
        var _send = XMLHttpRequest.prototype.send;
        XMLHttpRequest.prototype.send = function() {
            var _onload = this.onload;
            this.onload = function() {
                console.log(this.responseText);
                var data = JSON.parse(this.responseText);
                if (data.ext && (data.ext == 'xml' || data.ext == 'xml_client')) {
                    console.log("type=5")
                    intercept();
                } else if (data.ext && data.ext == 'link') {
                    console.log("type=6;" + data.url);
                    window.bridge.loadUrl(data.url);
                } else if (data.url) {
                    console.log('type=3;' + data.url);
                    window.bridge.loadVideo(data.url);
                }
            }
            return _send.apply(this, arguments);
        }
        player();
    } else {
        var a = document.getElementsByTagName('a');
        if (a.length > 0) {
            console.log("type=7;" + a[0].getAttribute('href'));
            window.bridge.reload(a[0].getAttribute('href'));
        } else
            window.bridge.destroy();
    }
} else if (window.vid) {
    console.log('type=4;' + vid);
    window.bridge.loadVideo(vid);
} else {
    var video = document.getElementsByTagName('video');
    if (video.length > 0)
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
function intercept() {
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
    }

    var observer = new window.MutationObserver(function (mutations) {
        scanPage();
    });

    observer.observe(document, {
        childList: true, // listen to changes to node children
        subtree: true // listen to changes to descendants as well
    });

    scanPage();
}
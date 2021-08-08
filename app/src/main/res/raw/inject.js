var mplayer = document.getElementsByClassName("mplayer");
if (mplayer.length > 0)
    var iframe = mplayer[0].getElementsByTagName("iframe");

if (iframe && iframe.length > 0) {
    window.bridge.loadUrl(iframe[0].getAttribute("src"));
} else if (window.config && config.url) {
    window.bridge.loadVideo(config.url);
} else if (window.player && player instanceof Function) {
    var _send = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.send = function() {
        var _onload = this.onload;
        this.onload = function() {
            console.log(this.responseText);
            var data = {}
            try {
                data = JSON.parse(this.responseText);
            } catch(e) {
                eval(this.responseText);
                if (window.tvInfoJs && tvInfoJs.data)
                    data = {"url":tvInfoJs.data.m3u};
            }
            if (data.ext && (data.ext == 'xml' || data.ext == 'xml_client')) {
                console.log("type=1");
                intercept();
            } else if (data.ext && data.ext == 'link') {
                console.log("type=2;" + data.url);
                window.bridge.loadUrl(data.url);
            } else if (data.url) {
                if (data.url.indexOf('//') == 0)
                    data.url = window.location.protocol + data.url;
                console.log('type=3;' + data.url);
                window.bridge.loadVideo(data.url);
            } else {
                console.log("type=4");
                intercept();
            }
        }
        return _send.apply(this, arguments);
    }
    player();
} else if (window.lele && lele.start) {
    function leleplayer(config) {
        console.log("type=5;" + config.video.url);
        window.bridge.loadVideo(config.video.url);
    }
    lele.start();
} else {
    intercept();
    scanFrame();
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
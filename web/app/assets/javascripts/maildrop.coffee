class Index
	suggestion: ->
		el = M.id "suggestion"
		cl = M.id "suggestion-icon"
		domain = el.getAttribute "data-domain"
		if el isnt null and domain isnt null
			cl.addClass "loading"
			M.getJSON "/api/suggestion?_=" + new Date().getTime(), (data) ->
				link = document.createElement("a")
				link.href = "/inbox/" + data.suggestion.toLowerCase()
				link.innerHTML = data.suggestion + "@" + domain
				el.removeChild(el.firstChild)
				el.appendChild(link)
				cl.removeClass "loading"

	init: =>
		p = M.id "suggestion-fill"
		if p isnt null then p.innerHTML = "<button id=suggestion-click>New Suggestion? <i id=suggestion-icon class=icon-arrows-ccw></i></button>"
		el = M.id "suggestion-click"
		if el isnt null
			el.onclick = =>
				@suggestion()
				false
		if M.id("clipboard") isnt null
			new ZeroClipboard(M.id("clipboard"), {
				moviePath: "/assets/ZeroClipboard.swf"
				trustedDomains: undefined,
				hoverClass: "clipboard-hover"
				activeClass: "clipboard-active"
				allowScriptAccess: "sameDomain"
				useNoCache: true
			})
		if M.id("global-zeroclipboard-flash-bridge") is null
			for c in M.cn("clipboard-controls")
				c.style.display = 'none'
		if window.WebSocket?
			@blocked()

	blocked: ->
		el = M.id "blocked"
		trash = M.id "trash"
		wshost = window['MailDropWsHost'] || window.location.host
		socket = new WebSocket("wss://" + wshost + "/ws/blocked")
		socket.onmessage = (msg) ->
			if document.all then el.innerText = msg.data else el.textContent = msg.data
			el.addClass "add"
			trash.addClass "bright"
			delay 1000, ->
				el.removeClass "add"
				trash.removeClass "bright"


class Inbox
	constructor: () ->
		@el = null
	init: =>
		window.__maildrop_iframe = ->
			f = document.getElementById("messageframe");
			if f isnt null
				f.height = 0
				if f.contentDocument
					f.height = f.contentDocument.documentElement.scrollHeight + 30
					if f.height < 300
						f.height = 300
				else
					f.height = f.contentWindow.document.body.scrollHeight + 30
		@el = M.id "inbox"
		reload = M.id "inbox-reload"
		if @el isnt null
			@inbox = @el.getAttribute "data-inbox"
			if @inbox isnt null
				if reload isnt null then reload.onclick = =>
					@reloadInbox()
					false
		if M.id("clipboard") isnt null
			new ZeroClipboard(M.id("clipboard"), {
				moviePath: "/assets/ZeroClipboard.swf"
				trustedDomains: undefined,
				hoverClass: "clipboard-hover"
				activeClass: "clipboard-active"
				allowScriptAccess: "sameDomain"
				useNoCache: true
			})
		if M.id("clipboard2") isnt null
			new ZeroClipboard(M.id("clipboard2"), {
				moviePath: "/assets/ZeroClipboard.swf"
				trustedDomains: undefined,
				hoverClass: "clipboard-hover"
				activeClass: "clipboard-active"
				allowScriptAccess: "sameDomain"
				useNoCache: true
			})
		if M.id("global-zeroclipboard-flash-bridge") is null
			for c in M.cn("clipboard-controls")
				c.style.display = 'none'
		tbl = M.id "inboxtbl"
		if tbl isnt null
			rows = tbl.querySelectorAll ".subject"
			for row in rows
				link = row.getElementsByTagName("a")
				if link isnt null
					link[0].onclick = ->
						clickMessage this
	reloadInbox: () =>
		tbl = M.id "inboxtbl"
		btn = M.id "reload-icon"
		if btn isnt null
			btn.removeClass "loading"
			btn.addClass "loading"
		M.getJSON("/api/inbox/" + @inbox + "?_=" + new Date().getTime(), (data) =>
			if btn isnt null
				btn.removeClass "loading"
			msgs = M.id "msgnum"
			txt = "" + data.length + " " + (if data.length is 1 then "message" else "messages") + "."
			if document.all then msgs.innerText = txt else msgs.textContent = txt
			tbl.removeClass "fade"
			tbl.addClass "fade"
			if data?.length > 0
				tbl.removeClass "empty"
				tbl.removeClass "len"
				tbl.addClass "len"
				tbody = tbl.getElementsByTagName("tbody")[0]
				tbody.innerHTML = ""
				for d in data
					tr = M.ce "tr"
					tr.setAttribute("data-id", d.id)
					sender = M.ce "td"
					sender.className = "sender"
					if document.all then sender.innerText = d.sender else sender.textContent = d.sender
					tr.appendChild sender
					subject = M.ce "td"
					subject.className = "subject"
					link = M.ce "a"
					link.href = "/inbox/" + @inbox + "/" + d.id
					if document.all then link.innerText = d.subject else link.textContent = d.subject
					link.onclick = -> clickMessage this
					subject.appendChild link
					tr.appendChild subject
					dt = M.ce "td"
					dt.className = "date"
					if document.all then dt.innerText = d.date else dt.textContent = d.date
					tr.appendChild dt
					tbody.appendChild tr
					delay 1000, => tbl.removeClass "fade"
			else
				tbl.removeClass "empty"
				tbl.removeClass "len"
				tbl.addClass "empty"
		)

clickMessage = (link) ->
	row = link.parentNode.parentNode
	oldrow = M.id "messagebody"
	if oldrow isnt null
		row.parentNode.removeChild oldrow
	nextrow = row.nextSibling
	newrow = M.ce "tr"
	newrow.id = "messagebody"
	cell = M.ce "td"
	cell.setAttribute("colspan", "3")
	cell.innerHTML = " \n<div id=\"buttons\"><a id=raw href=\"" + link + "/raw\">View Raw Source <i class=icon-doc-text></i></a>" +
		"<a id=close href=\"" + document.location + "\">Close <i class=icon-inbox></i></a>" +
		"<button id=delete type=submit data-url=\"/inbox/" + M.id("inbox").getAttribute("data-inbox") + "/" + row.getAttribute("data-id") + "\">Delete <i class=icon-trash></i></button></div>" +
		"<iframe " + "frameborder=\"0\" border=\"0\" id=\"messageframe\" src=\"" + link + "/body\" onload=\"__maildrop_iframe()\"></iframe>"
	newrow.appendChild cell
	if nextrow isnt null
		row.parentNode.insertBefore(newrow, nextrow)
	else
		row.parentNode.appendChild(newrow)
	# View raw source event
	raw = M.id "raw"
	raw.onclick = ->
		frame = M.id "messageframe"
		if frame isnt null
			if frame.src.endsWith("/raw")
				frame.src = this.href.replace(/\/raw$/, "/body")
				this.innerHTML = "View Raw Source <i class=icon-doc-text></i>"
			else
				frame.src = this.href
				this.innerHTML = "View Message <i class=icon-doc-text></i>"
		false
	close = M.id "close"
	close.onclick = ->
		frame = M.id "messagebody"
		if frame isnt null
			frame.parentNode.removeChild frame
		false
	# Delete button event
	del = M.id "delete"
	del.onclick = ->
		frame = M.id "messagebody"
		if frame isnt null
			frame.parentNode.removeChild frame
			# DELETE request
			M.deleteJSON("/api" + this.getAttribute("data-url"), ->
				# Reload the inbox
				M.id("inbox-reload").onclick()
			)
		false
	false


class Message
	constructor: () ->
		@el = null
	resize: =>
		if @el isnt null
			if @el.contentDocument
				@el.height = @el.contentDocument.documentElement.scrollHeight + 30
			else
				@el.height = @el.contentWindow.document.body.scrollHeight + 30
	init: =>
		@el = M.id "messageframe"
		if M.id("clipboard2") isnt null
			new ZeroClipboard(M.id("clipboard2"), {
				moviePath: "/assets/ZeroClipboard.swf"
				trustedDomains: undefined,
				hoverClass: "clipboard-hover"
				activeClass: "clipboard-active"
				allowScriptAccess: "sameDomain"
				useNoCache: true
			})
		if M.id("global-zeroclipboard-flash-bridge") is null
			for c in M.cn("clipboard-controls")
				c.style.display = 'none'


class Maildrop
	id: (i) -> document.getElementById i
	cn: (i) ->
		if document.getElementsByClassName
			document.getElementsByClassName i
		else
			@q("." + i)
	q: (i) -> document.querySelectorAll i
	ce: (i) -> document.createElement i

	getJSON: (url, f) ->
		if window.JSON
			ajax = new XMLHttpRequest()
			ajax.open "GET", url, true
			ajax.onreadystatechange = ->
				if ajax.readyState is 4 and ajax.status is 200
					f(JSON.parse(ajax.responseText))
			ajax.send()
	getHTML: (url, f) ->
		ajax = new XMLHttpRequest()
		ajax.open "GET", url, true
		ajax.onreadystatechange = ->
			if ajax.readyState is 4 and ajax.status is 200
				f(ajax.responseText)
		ajax.send()
	deleteJSON: (url, f) ->
		if window.JSON
			ajax = new XMLHttpRequest()
			ajax.open "DELETE", url, true
			ajax.onreadystatechange = ->
				if ajax.readyState is 4 and ajax.status is 200
					f(ajax.responseText)
			ajax.send()
	domReady: ->
		html = document.getElementsByTagName("html")[0]
		html.addClass "js domready"
		html.removeClass "no-js"
		if @id("template-index") isnt null then new Index(1).init()
		if @id("template-inbox") isnt null then new Inbox(1).init()
		if @id("template-message") isnt null then new Message(1).init()
		navform = @id("nav-form")
		if navform isnt null
			inbox = getCookie("inbox")
			if inbox isnt null then navform.querySelectorAll(".mailbox")[0].value = inbox


M = new Maildrop(1)

Element.prototype.addClass = (className) -> this.className += " " + className

Element.prototype.removeClass = (className) ->
	c = this.className.split(" ")
	this.className = (el for el in c when el isnt className).join(" ")

String.prototype.endsWith = (suffix) -> this.indexOf(suffix, this.length - suffix.length) isnt -1

delay = (ms, func) -> setTimeout func, ms
recurring = (ms, func) -> setInterval func, ms

getCookie = (key) ->
	c_value = document.cookie
	c_start = c_value.indexOf(" " + key + "=")
	if c_start is -1 then c_start = c_value.indexOf(key + "=")
	if c_start is -1
		c_value = null
	else
		c_start = c_value.indexOf("=", c_start) + 1
		c_end = c_value.indexOf(";", c_start)
		if c_end is -1 then c_end = c_value.length
		c_value = decodeURIComponent(c_value.substring(c_start, c_end))
	c_value


if document.addEventListener
	document.addEventListener "DOMContentLoaded", ->
		document.removeEventListener "DOMContentLoaded", arguments.callee, false
		M.domReady()
	, false
else if document.attachEvent
	document.attachEvent "onreadystatechange", ->
		if document.readyState is "complete"
			document.detachEvent "onreadystatechange", arguments.callee
			M.domReady()

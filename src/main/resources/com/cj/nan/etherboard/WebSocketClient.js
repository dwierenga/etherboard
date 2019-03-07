/*
 * Copyright (C) 2011, 2012 Commission Junction
 *
 * This file is part of etherboard.
 *
 * etherboard is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * etherboard is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with etherboard; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
/*global window console WebSocket WebSocketFactory*/


// this function will use WebSocketFactory, if it is available AND if standard WebSocket is not available
// WebSocketFactory is provided by the Etherboard Android application
(function () {
	var global = window, WebSocket;

    if (global.WebSocket || !global.WebSocketFactory) {
        return;
    }

	// WebSocket Object. All listener methods are cleaned up!
	WebSocket = global.WebSocket = function (url) {
		this.socket = WebSocketFactory.getInstance(url);
		if (this.socket) {
			WebSocket.store[this.socket.getId()] = this;
		} else {
			throw new Error('Websocket instantiation failed! Address might be wrong.');
		}
	};

	// storage to hold websocket object for later invokation of event methods
	WebSocket.store = {};

	// static event methods to call event methods on target websocket objects
	WebSocket.onmessage = function (evt) {
		WebSocket.store[evt._target].onmessage.call(global, evt);
	};	

	WebSocket.onopen = function (evt) {
		WebSocket.store[evt._target].onopen.call(global, evt);
	};

	WebSocket.onclose = function (evt) {
		WebSocket.store[evt._target].onclose.call(global, evt);
	};

	WebSocket.onerror = function (evt) {
		WebSocket.store[evt._target].onerror.call(global, evt);
	};

	// instance event methods
	WebSocket.prototype.send = function (data) {
		this.socket.send(data);
	};

	WebSocket.prototype.close = function () {
		this.socket.close();
	};

	WebSocket.prototype.getReadyState = function () {
		this.socket.getReadyState();
	};

	///////////// Must be overloaded
	WebSocket.prototype.onopen = function () {
		throw new Error('onopen not implemented.');
    };
    
    // alerts message pushed from server
    WebSocket.prototype.onmessage = function (msg) {
        throw new Error('onmessage not implemented.');
    };
    
    // alerts message pushed from server
    WebSocket.prototype.onerror = function (msg) {
        throw new Error('onerror not implemented.');
    };
    
    // alert close event
    WebSocket.prototype.onclose = function () {
        throw new Error('onclose not implemented.');
    };
}());


function WebSocketClient(url, handlers) {
	console.log("WebSocketClient: " + url);
    var socket;

    if (!window.WebSocket && window.MozWebSocket) {
        window.WebSocket = window.MozWebSocket;
    }

    if (window.WebSocket) {
        socket = new WebSocket(url);
        socket.onopen = handlers.onOpen || function () {};
        socket.onmessage = handlers.onMessage || function () {};
        socket.onclose = handlers.onClose || function () {};
    } else {
        console.log("Your browser does not support Web Socket.");
    }

    return {
        send: function (message) {
            if (socket.readyState === WebSocket.OPEN) {
                socket.send(message);
            } else {
                console.log("The socket is not open.");
            }
        }
    };
}

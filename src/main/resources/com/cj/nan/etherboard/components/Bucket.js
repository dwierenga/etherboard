/*jslint newcap: false*/
/*global $ console window Issue StickyEditor confirm*/

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

function Bucket(bucket, parent, boardId, createIssueCallback, webSocketClient) {
    var bucketId = 'bucket' + bucket.id,
        widget = $('<div id= ' + bucketId + ' class="bucket">' +
                	   '<div class="bucketHeader"></div>' +
                       '<div class="bucketControls">' +
                           '<img class="stickyEditButton" title="Edit" src="pencil.png" />' +
                           '<img class="stickyCloseButton" title="Delete" src="close_icon.gif" />' +
                           '<div style="clear:both"></div>' +
                       '</div>' +
                       '<div class="bucketListHolder"><ol class="bucketList"></ol></div>' +
                   '</div>').css(bucket.pos).appendTo(parent);

    function doSave() {
        $.ajax('/board/' + boardId + '/objects/' + bucket.id, {
            dataType: 'json',
            data: JSON.stringify(bucket),
            type: 'PUT',
            success: function (createdObject) {
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.log("ERROR:" + textStatus);
            }
        });
    }

    function bucketSave() {
        bucket.contents = [];
        widget.find(".bucketList > li > span").each(function () {
            bucket.contents.push({ name: $(this).data("content"), extraNotes: $(this).data("extraNotes") });
        });
        doSave();
    }

    function update() {
        var bucketList = widget.find('.bucketList').empty().sortable({
                stop: function () {
                    bucketSave();
                }
            }), i;

        widget.find('.bucketHeader').html(bucket.name);
        widget.height(bucket.height || 150);
        widget.width(bucket.width || 150);

        for (i = 0; i < bucket.contents.length; i++) {
        	
        	var text = $(bucket.contents[i].name).text()
        	var content = text.split("\n")[0];
        	
            var item = $("<li></li>").append(
			    $("<span></span>").html(content)
		    ).append("<a href='#' class='remove'>--&gt;</a>").appendTo(bucketList).find("span");
            item.data("extraNotes", bucket.contents[i].extraNotes);
            item.data("content", bucket.contents[i].name);
        }

        bucketList.find(".remove").click(function (e) {
            var sticky = $(this).parent().find("span"),
                name = sticky.data("content"),
                extraNotes = sticky.data("extraNotes");

            createIssueCallback({ name: name, extraNotes: extraNotes, kind: "sticky", pos: { top: $(this).offset().top - 75, left: $(this).offset().left - 75 } });
            $(this).parent().remove();

            webSocketClient.send(JSON.stringify({
                type: "removeBucketItem",
                widgetId: bucketId,
                content: { name: name, extraNotes: extraNotes }
            }));

            bucketSave();
            
            e.preventDefault();
        });
    }

    widget.bind('addBucketItem', function (event, bucketItem) {
        bucket.contents.push(bucketItem);
        update();
    });

    widget.bind('removeBucketItem', function (event, bucketItem) {
        var indexToRemove = -1;

        $.each(bucket.contents, function (index, item) {
            if (bucketItem.name === item.name && bucketItem.extraNotes === item.extraNotes) {
                indexToRemove = index;
            }
        });

        console.log("remove: ");
        console.dir(bucketItem);
        console.log("contents: ");
        console.dir(bucket.contents);
        console.log("index: " + indexToRemove);

        if (indexToRemove !== -1) {
            bucket.contents.splice(indexToRemove, 1);
        }
        update();
    });

    update();

    widget.draggable({
            containment: [0, 0, Infinity, Infinity],
            drag: function (event, ui) {
                var msg = {
                    type: "positionChange",
                    widgetId: bucketId,
                    position: widget.offset()
                };
                webSocketClient.send(JSON.stringify(msg));
            },
            stop: function (event, ui) {
                event.stopPropagation();
                bucket.pos = widget.offset();
                doSave();
            }
        })
        .droppable({
            drop: function (event, ui) {
                var sticky = $(ui.draggable);
                event.stopPropagation();

                if (sticky.hasClass("sticky")) {
                    sticky.data("living", false);
                    sticky.trigger("deleteSticky");
                    bucket.contents.push({name: sticky.find(".stickyContent").html(), extraNotes: sticky.find(".extraNotes").html()});

                    webSocketClient.send(JSON.stringify({
                        type: 'addBucketItem',
                        widgetId: bucketId,
                        itemContents: {name: sticky.find(".stickyContent").html(), extraNotes:  sticky.find(".extraNotes").html()}
                    }));

                    doSave();
                    update();
                }
            }
        })
        .resizable({
            minHeight: 150,
            minWidth: 150,
            stop: function (event, ui) {
                bucket.width = widget.width();
                bucket.height = widget.height();
                widget.find(".bucketControls").width(widget.width());

                doSave();
                update();
            }
        });

    widget.find('.stickyEditButton').click(function (event) {
        StickyEditor(bucket, parent, function (newSticky) {
            doSave();
            update();
            var titleChangeMessage = {
                type: "bucketContentChanged",
                widgetId: bucketId,
                content: bucket.name
            };
            webSocketClient.send(JSON.stringify(titleChangeMessage));
        });
    });

    widget.find('.stickyCloseButton').click(function () {
        if (confirm(" )': DELETE is permanent! :'( ")) {
            $.ajax('/board/' + boardId + '/objects/' + bucket.id, {
                dataType: 'json',
                type: 'DELETE',
                success: function (createdObject) {
                    widget.remove();
                    var msg = {
                        type: "deleteWidget",
                        widgetId: bucketId
                    };
                    webSocketClient.send(JSON.stringify(msg));
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    console.log("ERROR:" + textStatus);
                }
            });
        }
    });

    widget.find(".stickyHeader").width(widget.width() + 4);

    widget.bind("setContents", function (event, bucketInfo) {
        bucket.name = bucketInfo.content;
        update();
    });

    widget.on("touchstart touchmove touchend touchcancel", function (ev) {
        var event = ev.originalEvent,
            touches = event.changedTouches,
            first = touches[0],
            simulatedEvent = document.createEvent("MouseEvent"),
            types = {touchstart: "mousedown", touchmove: "mousemove", touchend: "mouseup"},
            type = types[event.type];

        if (type) {
            simulatedEvent.initMouseEvent(type, true, true, window, 1,
                first.screenX, first.screenY,
                first.clientX, first.clientY, false,
                false, false, false, 0, null);
            first.target.dispatchEvent(simulatedEvent);
            event.preventDefault();
        }
    });
}

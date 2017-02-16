$(function() {
	var url = window.location.pathname + window.location.search;
	//根据当前url设置菜单为激活状态
	var menuItem = $('.sidebar-menu a[href="' + url + '"]');
   	menuItem.parent().addClass('active');
   	menuItem.parent().parent().parent('.treeview').addClass('active');
});
(function() {
	//覆盖验证框架的显示错误信息的缺省实现
	if($.validator) {
		$.validator.prototype.backupShowErrors = $.validator.prototype.defaultShowErrors;
    	$.validator.prototype.defaultShowErrors = function() {
    		var errorContainer = $('.error-container', this.currentForm);
    		var closeBtn = errorContainer.children('button.close');
    		//如果当前FORM下，没有error-container这个元素，则调用原始的实现
    		if(!errorContainer.get(0)) {
    			$.validator.prototype.backupShowErrors.call(this);
    			return;
    		}
    		var labels = [];
    		if(!this.errorList || this.errorList.length == 0) {
    			errorContainer.hide('fast');
    			return;
    		}
    		for (var i = 0; this.errorList[i]; i++ ) {
				var error = this.errorList[i];
				var name = $(this.errorList[i].element).attr('data-label');
				if(!name && name != '') {
					name = $(this.errorList[i].element).attr('name');
				}
				labels.push("<div class='error' data-name='" + $(this.errorList[i].element).attr('name') + "'>" + name + error.message + "</div>")
    		}
    		if(!closeBtn.get(0)) { //没有关闭按钮
    		  errorContainer.html(labels.join(''))
    		} else { //有关闭按钮
    			var childCtn = errorContainer.children('div.error-inner');
    			if(!childCtn.get(0)) {
    				$('<div class="error-inner"></div>').appendTo(errorContainer);
    			}
    			childCtn = errorContainer.children('div.error-inner');
    			childCtn.html(labels.join(''));
    			closeBtn.children('span').unbind();
    			closeBtn.unbind().click(function() {
    				errorContainer.hide('fast');
    			});
    		}
    		errorContainer.show('fast');
    		
    	}
    }
}
    
)();
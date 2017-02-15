$(function() {
	var url = window.location.pathname + window.location.search;
	//根据当前url设置菜单为激活状态
	var menuItem = $('.sidebar-menu a[href="' + url + '"]');
   	menuItem.parent().addClass('active');
   	menuItem.parent().parent().parent('.treeview').addClass('active');
});
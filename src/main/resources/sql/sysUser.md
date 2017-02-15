byMobile
===
* 根据手机号码查询用户

    select #use("cols")# from sys_user where mobile=#mobile#

sample
===
* 注释

	select #use("cols")# from sys_user where #use("condition")#

cols
===

	username,password,mobile,email,company,real_name,is_available,regist_time,id,name,descn

updateSample
===

	`username`=#username#,`password`=#password#,`mobile`=#mobile#,`email`=#email#,`company`=#company#,`real_name`=#realName#,`is_available`=#isAvailable#,`regist_time`=#registTime#,`id`=#id#,`name`=#name#,`descn`=#descn#

condition
===

	1 = 1  
	@if(!isEmpty(username)){
	 and `username`=#username#
	@}
	@if(!isEmpty(password)){
	 and `password`=#password#
	@}
	@if(!isEmpty(mobile)){
	 and `mobile`=#mobile#
	@}
	@if(!isEmpty(email)){
	 and `email`=#email#
	@}
	@if(!isEmpty(company)){
	 and `company`=#company#
	@}
	@if(!isEmpty(realName)){
	 and `real_name`=#realName#
	@}
	@if(!isEmpty(isAvailable)){
	 and `is_available`=#isAvailable#
	@}
	@if(!isEmpty(registTime)){
	 and `regist_time`=#registTime#
	@}
	@if(!isEmpty(name)){
	 and `name`=#name#
	@}
	@if(!isEmpty(descn)){
	 and `descn`=#descn#
	@}
	

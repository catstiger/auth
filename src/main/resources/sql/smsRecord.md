makeUnavailable
===
* 将发送给某个手机的验证短信，全部设置为失效
    
    update sms_record set is_available=false where type='verify' and mobile=#mobile#

lastAvailable
===
* 返回最后一个有效的验证短信

   select #use("cols")# from sms_record where is_available=true and type='verify' and mobile=#mobile# order by send_time desc limit 1

sample
===
* 注释

	select #use("cols")# from sms_record where #use("condition")#

cols
===

	user_id,username,mobile,template_id,code,body,type,send_time,return_code,is_available,send_success,id

updateSample
===

	`user_id`=#userId#,`username`=#username#,`mobile`=#mobile#,`template_id`=#templateId#,`code`=#code#,`body`=#body#,`type`=#type#,`send_time`=#sendTime#,`return_code`=#returnCode#,`is_available`=#isAvailable#,`send_success`=#sendSuccess#,`id`=#id#

condition
===

	1 = 1  
	@if(!isEmpty(userId)){
	 and `user_id`=#userId#
	@}
	@if(!isEmpty(username)){
	 and `username`=#username#
	@}
	@if(!isEmpty(mobile)){
	 and `mobile`=#mobile#
	@}
	@if(!isEmpty(templateId)){
	 and `template_id`=#templateId#
	@}
	@if(!isEmpty(code)){
	 and `code`=#code#
	@}
	@if(!isEmpty(body)){
	 and `body`=#body#
	@}
	@if(!isEmpty(type)){
	 and `type`=#type#
	@}
	@if(!isEmpty(sendTime)){
	 and `send_time`=#sendTime#
	@}
	@if(!isEmpty(returnCode)){
	 and `return_code`=#returnCode#
	@}
	@if(!isEmpty(isAvailable)){
	 and `is_available`=#isAvailable#
	@}
	@if(!isEmpty(sendSuccess)){
	 and `send_success`=#sendSuccess#
	@}
	

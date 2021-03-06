package com.kim.web.user;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.kim.bean.Params;
import com.kim.bean.User;
import com.kim.service.user.IUserService;
import com.kim.web.BaseController;

/**
 * 用户模块web
 * UserController
 * 创建人:kim
 * 时间：2016年09月04日  11:53:31
 * @version 1.0.0
 */
@Controller
@RequestMapping("/admin/user")
public class AdminUserController extends BaseController{
	
	@Resource(name="userService")
	private IUserService userService;
	
	/**
	 * 跳转到列表页面
	 * @Title: list 
	 * @Description: TODO(这里用一句话描述这个方法的作用) 
	 * @param @param params
	 * @param @return  参数说明 
	 * @return String  返回类型 
	 * @throws
	 */
	@RequestMapping("/list")
	public String list(Params params){
		return "admins/user/list";
	}
	
	/**
	 * 查询所有User返回模板页面
	 * @Title: template 
	 * @Description: TODO(这里用一句话描述这个方法的作用) 
	 * @param @param params
	 * @param @return  参数说明 
	 * @return ModelAndView  返回类型 
	 * @throws
	 */
	@RequestMapping("/template")
	@ResponseBody
	public ModelAndView template(Params params){
		ModelAndView modelAndView = new ModelAndView();
		List<User> users = userService.findAllUsers(params);
		int count = userService.countUser(params);
		modelAndView.setViewName("admins/user/template");
		modelAndView.addObject("users",users);
		modelAndView.addObject("itemCount",count);
		return modelAndView;
	}
	/**
	 * 添加保存User
	 * @Title: saveUser 
	 * @Description: TODO(这里用一句话描述这个方法的作用) 
	 * @param @param user
	 * @param @return  参数说明 
	 * @return String  返回类型 
	 * @throws
	 */
	@ResponseBody
	@RequestMapping("/save")
	public String saveUser(User user){
		if(user != null){
			User userSession = (User) request.getSession().getAttribute("user");
			user.setUserId(userSession.getUserId());
			user.setName(userSession.getName());
			user.setIsDelete(0);
			boolean faly = userService.saveUser(user);
			if(faly){
				return "success";
			}else{
				return "fail";
			}
		}else{
			return "error";
		}
	}
	/**
	 * 查询User详情
	 * @Title: getUser 
	 * @Description: TODO(这里用一句话描述这个方法的作用) 
	 * @param @param params
	 * @param @return  参数说明 
	 * @return ModelAndView  返回类型 
	 * @throws
	 */
	@RequestMapping("/detail/{id}")
	public ModelAndView getUser(@PathVariable("id")Integer id,Params params){
		ModelAndView modelAndView = new ModelAndView();
		User user = userService.getUser(params);
		modelAndView.setViewName("user/detail");
		modelAndView.addObject("user",user);
		return modelAndView;
	}
	/**
	 * 更新User
	 * @Title: updateUser 
	 * @Description: TODO(这里用一句话描述这个方法的作用) 
	 * @param @param content
	 * @param @return  参数说明 
	 * @return String  返回类型 
	 * @throws
	 */
	@ResponseBody
	@RequestMapping(value="/update",method=RequestMethod.POST)
	public String updateUser(User user){
		return userService.updateUser(user)?"success":"fail";
	}
	
	/**
	 * 删除User
	 * @Title: deleteUser 
	 * @Description: TODO(这里用一句话描述这个方法的作用) 
	 * @param @param id
	 * @param @return  参数说明 
	 * @return String  返回类型 
	 * @throws
	 */
	@ResponseBody
	@RequestMapping(value="/delete",method=RequestMethod.POST)
	public String deleteUser(Params Params){
		return userService.deleteUser(Params)?"success":"fail";
	}
}

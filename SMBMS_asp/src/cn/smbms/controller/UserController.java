package cn.smbms.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.mysql.jdbc.StringUtils;

import cn.smbms.pojo.Role;
import cn.smbms.pojo.User;
import cn.smbms.service.role.RoleService;
import cn.smbms.service.user.UserService;
import cn.smbms.tools.Constants;
import cn.smbms.tools.PageSupport;



@Controller
@RequestMapping("/user")
public class UserController {
	private static final Object JSONArray = null;
	private Logger logger=Logger.getLogger(UserController.class);
	@Resource
	private UserService userService;
	
	@Resource
	private RoleService roleService;
	
	
	/**
	 * @author 
	 *跳转到登陆页面
	 */
	@RequestMapping("/login.html")
	public String login(){
		logger.debug("UserController welcome SMBMS系统==============");
		return "login";
	}
	
	/**
	 * @author ben
	 *处理登陆信息
	 */
	@RequestMapping(value="/dologin.html",method=RequestMethod.POST)
	public String doLogin(String userCode,String userPassword,
						HttpSession session,HttpServletRequest request){
		logger.debug("doLogin=================");
		User user=userService.login(userCode, userPassword);
		
		if(null!=user){
			session.setAttribute(Constants.USER_SESSION, user);
			return "redirect:/user/main.html";
		}else{
			request.setAttribute("error", "用户名或密码不正确");
			return "login";
		}
	}
	
	@RequestMapping(value="/main.html")
	public String main(){
		return "frame";
	}
	
	@RequestMapping(value="/exlogin.html",method=RequestMethod.GET)
	public String exLogin(String userCode, String userPassword){
		logger.debug("exLogin=================");
		User user=userService.login(userCode, userPassword);
		if(null==user){
			throw new RuntimeException("用户名或者密码错误");
		}
		return "redirect:/user/main.html";
	}
	
	//局部异常
	/*@ExceptionHandler(value={RuntimeException.class})
	public String handlerException(RuntimeException e,HttpServletRequest req){
		req.setAttribute("e", e);
		return "error";
	}*/
	
	@RequestMapping(value="/userlist.html")
	public String getUserList(Model model,  
								@RequestParam(value="queryname",required=false)String queryUserName,
								@RequestParam(value="queryUserRole",required=false)String queryUserRole,
								@RequestParam(value="pageIndex",required=false)String pageIndex){
		logger.debug("getUserList------->>queryName" +queryUserName);
		logger.debug("getUserList------->>queryUserRole" +queryUserRole);
		logger.debug("getUserList------->>pageIndex" +pageIndex);
		int _queryUserRole=0;
		List<User> userList=null;
		//设置页面容量
		int pageSize=Constants.pageSize;
		//当前页码
		int currentPageNo=1;
		if(queryUserName==null){
			queryUserName="";
		}
		if(queryUserRole!=null && !queryUserRole.equals("")){
			_queryUserRole=Integer.parseInt(queryUserRole);
		}
		if(pageIndex!=null){
			try {
				currentPageNo=Integer.valueOf(pageIndex);
			} catch (Exception e) {
				// TODO: handle exception
				return "redirect:/user/syserror.html";
			}
		}
		
		//总数量（表）
		int totalCount=userService.getUserCount(queryUserName,_queryUserRole);
		//总页数
		PageSupport pages=new PageSupport();
		pages.setCurrentPageNo(currentPageNo);
		pages.setPageSize(pageSize);
		pages.setTotalCount(totalCount);
		int totalPageCount=pages.getTotalPageCount();
		//控制首页和尾页
		if(currentPageNo<1){
			currentPageNo=1;
		}else if(currentPageNo>totalPageCount){
			currentPageNo=totalPageCount; 
		}
		userList=userService.getUserList(queryUserName, _queryUserRole, currentPageNo, pageSize);
		model.addAttribute("userList",userList);
		List<Role> roleList=null;
		roleList=roleService.getRoleList();
		model.addAttribute("roleList", roleList);
		model.addAttribute("queryUserName", queryUserName);
		model.addAttribute("queryUserRole", queryUserRole);
		model.addAttribute("totalPageCount", totalPageCount);
		model.addAttribute("totalCount", totalCount);
		model.addAttribute("currentPageNo", currentPageNo);
		
		return "userlist";
		
	}
	
	@RequestMapping(value="/syserror.html")
	public String sysError(){
		return "syserror";
	}
	
	@RequestMapping(value="/useradd.html",method=RequestMethod.GET)
	public String addUser(@ModelAttribute("user") User user){
		return "useradd";
	}
	
	@RequestMapping(value="/addsave.html",method=RequestMethod.POST)
	public String addUserSave(User user,HttpSession session){
		user.setCreatedBy(((User)session.getAttribute(Constants.USER_SESSION)).getId());
		user.setCreationDate(new Date());
		if(userService.add(user)){
			return "redirect:/user/userlist.html";
		}
		return "useradd";
	}
	
	/**
	 * @author ben
	 *增加供应商管理信息
	 */
	
	//增加用户信息
	@RequestMapping(value="/add.html",method=RequestMethod.GET)
	public String add(@ModelAttribute("user") User user){
		return "user/useradd";
	}
	
	@RequestMapping(value="/add.html",method=RequestMethod.POST)
	public String addSave(@Valid User user,BindingResult bindingResult,
							HttpSession session){
		if(bindingResult.hasErrors()){
			logger.debug("add user validated has error============");
			return "user/useradd";
		}
		user.setCreatedBy(((User)session.getAttribute(Constants.USER_SESSION)).getId());
		user.setCreationDate(new Date());
		if(userService.add(user)){
			return "redirect:/user/userlist.html";
		}
		return "user/useradd";
	}
	
	/**
	 * @author ben
	 *修改用户信息
	 */
	@RequestMapping(value="/usermodify.html",method=RequestMethod.GET)
	public String getUserByld(@RequestParam String uid,Model model){
		logger.debug("getUserById uid============"+uid);
		User user=userService.getUserById(uid);
		model.addAttribute(user);
		return "usermodify";
		
	}
	
	@RequestMapping(value="/usermodify.html",method=RequestMethod.POST)
	public String modifyUserSave(User user,HttpSession session){
		logger.debug("modifyUserSave userid=============="+user.getId());
		user.setModifyBy(((User)session.getAttribute(Constants.USER_SESSION)).getId());
		user.setModifyDate(new Date());
		if(userService.modify(user)){
			return "redirect:/user/userlist.html";
		}
		return "usermodify";
	}
	
	/**
	 * @author ben
	 *增加查看明细处理
	 */
	public String view(@PathVariable String id,Model model){
		logger.debug("view id============================"+id);
		User user=userService.getUserById(id);
		model.addAttribute(user);
		return "userview";
	}
	
	
	
	/**
	 * @author ben
	 *异步刷新
	 */
	@RequestMapping(value="/usexist.html")
	public Object userCodeksExit(@RequestParam String userCode){
		HashMap<String, String>resultMap=new HashMap<String, String>();
		if(StringUtils.isNullOrEmpty(userCode)){
			resultMap.put("userCode", "exist");
		}else{
			User user=userService.selectUserCodeExist(userCode);
			if(null!=user){
				resultMap.put("userCode", "exist");
			}else{
				resultMap.put("userCode", "noexits");
			}
		}
		return com.alibaba.fastjson.JSONArray.toJSONString(resultMap);
	}
}
package cn.edu.cqu.Recommend.Service.ServiceImpl;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.stereotype.Component;

import cn.edu.cqu.Recommend.Dao.SearchRecordMapper;
import cn.edu.cqu.Recommend.Dao.UserMapper;
import cn.edu.cqu.Recommend.Dao.ViewRecordMapper;
import cn.edu.cqu.Recommend.Pojo.SearchRecord;
import cn.edu.cqu.Recommend.Pojo.User;
import cn.edu.cqu.Recommend.Pojo.UserExample;
import cn.edu.cqu.Recommend.Pojo.ViewRecord;
import cn.edu.cqu.Recommend.Service.ActionService;
import cn.edu.cqu.Recommend.Utils.MyJson;
import cn.edu.cqu.Recommend.Utils.Static.LogioStrings;
import cn.edu.cqu.Recommend.Utils.Static.UserInfoStrings;

@Component
public class ActionServiceImpl implements ActionService {

	@Autowired
	UserMapper userMapper;
	@Autowired
	ViewRecordMapper viewRecordMapper;
	@Autowired
	SearchRecordMapper searchRecordMapper;

	@Override
	public MyJson login(User user, HttpSession session) {
		UserExample userExample = new UserExample();
		userExample.or().andUserTelEqualTo(user.getUserTel());
		List<User> users = userMapper.selectByExample(userExample);
		if (users.size() == 0) {
			// 无此账号
			return new MyJson(false, LogioStrings.NO_ACCOUNT);
		}
		if (!user.getUserPassword().equals(users.get(0).getUserPassword())) {
			// 密码错误
			return new MyJson(false, LogioStrings.WRONG_PASSWORD);
		}
		// 密码正确，建立session
		session.setAttribute("user", user);
		return new MyJson(true, LogioStrings.LOGIN_SUCCESS);
	}

	@Override
	public MyJson logout(HttpSession session) {
		session.invalidate();
		return new MyJson(true, LogioStrings.LOGOUT_SUCCESS);
	}

	@Override
	public MyJson signup(User user) {
		UserExample userExample = new UserExample();
		userExample.or().andUserTelEqualTo(user.getUserTel());
		List<User> users = userMapper.selectByExample(userExample);
		if (users.size() != 0) {
			// 手机号已被注册
			return new MyJson(false, UserInfoStrings.TEL_EXIST);
		}
		try {
			// 注册成功
			userMapper.insert(user);
			return new MyJson(true, UserInfoStrings.SIGNUP_SUCCESS);
		} catch (Exception e) {
			// 注册失败
			System.err.println(e);
			return new MyJson(false, UserInfoStrings.SIGNUP_FAILED);
		}
	}

	@Override
	public boolean viewLog(HttpSession session, Integer movieId) {
		User user = (User) session.getAttribute("user");
		if (user == null) {
			// 未登录
			return false;
		}
		ViewRecord viewRecord = new ViewRecord();
		viewRecord.setMovieId(movieId);
		viewRecord.setUserId(user.getUserId());
		viewRecord.setUserTel(user.getUserTel());
		viewRecord.setViewRecordTime(new Date());
		try {
			viewRecordMapper.insert(viewRecord);
			return true;
		} catch (Exception e) {
			System.err.println(e);
			return false;
		}
	}

	@Override
	public boolean searchLog(HttpSession session, String input) {
		User user = (User) session.getAttribute("user");
		if (user == null) {
			// 未登录
			return false;
		}
		SearchRecord searchRecord = new SearchRecord();
		searchRecord.setSearchRecordItem(input);
		searchRecord.setSearchRecordTime(new Date());
		searchRecord.setUserId(user.getUserId());
		searchRecord.setUserTel(user.getUserTel());
		try {
			searchRecordMapper.insert(searchRecord);
			return true;
		} catch (Exception e) {
			System.err.println(e);
			return false;
		}
	}

}

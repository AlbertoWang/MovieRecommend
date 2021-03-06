package cn.edu.cqu.Recommend.Service.ServiceImpl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import cn.edu.cqu.Recommend.Dao.CinemaMapper;
import cn.edu.cqu.Recommend.Dao.LikeRecordMapper;
import cn.edu.cqu.Recommend.Dao.MovieInfoMapper;
import cn.edu.cqu.Recommend.Dao.MovieInfoRecommendMapper;
import cn.edu.cqu.Recommend.Dao.TimelyMovieMapper;
import cn.edu.cqu.Recommend.Dao.TimelySessionMapper;
import cn.edu.cqu.Recommend.Pojo.Cinema;
import cn.edu.cqu.Recommend.Pojo.CinemaExample;
import cn.edu.cqu.Recommend.Pojo.CinemaSession;
import cn.edu.cqu.Recommend.Pojo.LikeRecord;
import cn.edu.cqu.Recommend.Pojo.LikeRecordExample;
import cn.edu.cqu.Recommend.Pojo.MovieInfo;
import cn.edu.cqu.Recommend.Pojo.MovieInfoExample;
import cn.edu.cqu.Recommend.Pojo.MovieInfoRecommendExample;
import cn.edu.cqu.Recommend.Pojo.MovieInfoRecommendWithBLOBs;
import cn.edu.cqu.Recommend.Pojo.MovieInfoWithBLOBs;
import cn.edu.cqu.Recommend.Pojo.TimelyMovieExample;
import cn.edu.cqu.Recommend.Pojo.TimelySession;
import cn.edu.cqu.Recommend.Pojo.TimelySessionExample;
import cn.edu.cqu.Recommend.Pojo.User;
import cn.edu.cqu.Recommend.Service.UserService;
import cn.edu.cqu.Recommend.Utils.MyJson;
import cn.edu.cqu.Recommend.Utils.Static.ErrInfoStrings;
import cn.edu.cqu.Recommend.Utils.Static.LogioStrings;

@Component
public class UserServiceImpl implements UserService {

	private final String TODAY = "TODAY";
	private final String TOMORROW = "TOMORROW";

	@Autowired
	CinemaMapper cinemaMapper;
	@Autowired
	MovieInfoMapper movieInfoMapper;
	@Autowired
	TimelyMovieMapper timelyMovieMapper;
	@Autowired
	TimelySessionMapper timelySessionMapper;
	@Autowired
	MovieInfoRecommendMapper movieInfoRecommendMapper;
	@Autowired
	LikeRecordMapper likeRecordMapper;

	@Override
	public MyJson getUserInfo(HttpSession session) {
		User user = (User) session.getAttribute("user");
		if (user == null) {
			// 未登录用户
			return new MyJson(false, LogioStrings.NOT_LOGIN);
		}
		// 已登录用户
		return new MyJson(true, user);
	}

	@SuppressWarnings({ "deprecation", "static-access" })
	@Override
	public MyJson getTimelySession(Integer movieId, String condition) {
		TimelySessionExample timelySessionExample = new TimelySessionExample();
		CinemaExample cinemaExample = new CinemaExample();
		cinemaExample.or();
		Date time_now = new Date();
		if (condition.equals(TODAY)) {
			// 查询今日有效电影场次
			timelySessionExample.or()
					.andSessionStartLessThan(
							new Date(time_now.getYear(), time_now.getMonth(), time_now.getDate(), 23, 59, 59))
					.andMovieIdEqualTo(movieId);
		} else if (condition.equals(TOMORROW)) {
			// 查询明日有效电影场次
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(time_now);
			calendar.add(calendar.DATE, 1);
			time_now = calendar.getTime();
			timelySessionExample.or()
					.andSessionStartBetween(
							new Date(time_now.getYear(), time_now.getMonth(), time_now.getDate(), 0, 0, 0),
							new Date(time_now.getYear(), time_now.getMonth(), time_now.getDate(), 23, 59, 59))
					.andMovieIdEqualTo(movieId);
		}
		try {
			List<TimelySession> timelySessions = timelySessionMapper.selectByExample(timelySessionExample);
			List<Cinema> cinemas = cinemaMapper.selectByExample(cinemaExample);
			List<CinemaSession> cinemaSessions = new ArrayList<CinemaSession>();
			for (Cinema cinema : cinemas) {
				Iterator<TimelySession> iterator = timelySessions.iterator();
				List<TimelySession> tempList = new ArrayList<TimelySession>();
				while (iterator.hasNext()) {
					TimelySession timelySession = iterator.next();
					if (cinema.getCinemaId().equals(timelySession.getCinemaId())) {
						tempList.add(timelySession);
						iterator.remove();
					}
				}
				cinemaSessions.add(new CinemaSession(cinema, tempList));
			}
			// 去掉无场次影院
			Iterator<CinemaSession> iterator = cinemaSessions.iterator();
			while (iterator.hasNext()) {
				CinemaSession cinemaSession = iterator.next();
				if (cinemaSession.getSessions().size() == 0) {
					iterator.remove();
					continue;
				}
			}
			return new MyJson(true, cinemaSessions);
		} catch (Exception e) {
			System.err.println(e);
			return new MyJson(false, ErrInfoStrings.DATABASE_ERR);
		}
	}

	@Override
	public MyJson getMovies(int pageNum, int pageSize) {
		MovieInfoExample movieInfoExample = new MovieInfoExample();
		movieInfoExample.or();
		try {
			PageHelper.startPage(pageNum, pageSize);
			PageInfo<MovieInfoWithBLOBs> pageInfo = new PageInfo<MovieInfoWithBLOBs>(
					movieInfoMapper.selectByExampleWithBLOBs(movieInfoExample));
			return new MyJson(true, pageInfo);
		} catch (Exception e) {
			System.err.println(e);
			return new MyJson(false, ErrInfoStrings.DATABASE_ERR);
		}
	}

	@Override
	public MyJson addFavorite(Integer movieId, User user) {
		LikeRecord likeRecord = new LikeRecord();
		likeRecord.setMovieId(movieId);
		likeRecord.setUserId(user.getUserId());
		likeRecord.setUserTel(user.getUserTel());
		likeRecord.setLikeRecordTime(new Date());
		try {
			likeRecordMapper.insert(likeRecord);
			return new MyJson(true, "添加收藏成功");
		} catch (Exception e) {
			System.err.println(e);
			return new MyJson(false, ErrInfoStrings.DATABASE_ERR);
		}
	}

	@Override
	public MyJson removeFavorite(Integer movieId, User user) {
		LikeRecordExample likeRecordExample = new LikeRecordExample();
		likeRecordExample.or().andUserIdEqualTo(user.getUserId()).andMovieIdEqualTo(movieId);
		try {
			likeRecordMapper.deleteByExample(likeRecordExample);
			return new MyJson(true, "取消收藏成功");
		} catch (Exception e) {
			System.err.println(e);
			return new MyJson(false, ErrInfoStrings.DATABASE_ERR);
		}
	}

	@Override
	public MyJson getFavorite(User user) {
		LikeRecordExample likeRecordExample = new LikeRecordExample();
		likeRecordExample.or().andUserIdEqualTo(user.getUserId());
		try {
			List<LikeRecord> likeRecords = likeRecordMapper.selectByExample(likeRecordExample);
			return new MyJson(true, likeRecords);
		} catch (Exception e) {
			System.err.println(e);
			return new MyJson(false, ErrInfoStrings.DATABASE_ERR);
		}
	}

	@Override
	public MyJson isFavorite(Integer movieId, User user) {
		LikeRecordExample likeRecordExample = new LikeRecordExample();
		likeRecordExample.or().andUserIdEqualTo(user.getUserId()).andMovieIdEqualTo(movieId);
		try {
			List<LikeRecord> likeRecords = likeRecordMapper.selectByExample(likeRecordExample);
			// 已收藏
			if (likeRecords.size() != 0) {
				return new MyJson(true, true);
			}
			// 未收藏
			return new MyJson(true, false);
		} catch (Exception e) {
			System.err.println(e);
			return new MyJson(false, ErrInfoStrings.DATABASE_ERR);
		}
	}

	@Override
	public MyJson searchMovie(String condition) {
		condition = "%" + condition + "%";
		MovieInfoExample movieInfoExample = new MovieInfoExample();
		movieInfoExample.or().andMovieNameLike(condition);
		try {
			List<MovieInfoWithBLOBs> movieInfos = movieInfoMapper.selectByExampleWithBLOBs(movieInfoExample);
			List<Integer> movieIds = movieInfoMapper.selectMovieByActorAndDirector(condition);
			for (Integer movieId : movieIds) {
				movieInfoExample.clear();
				movieInfoExample.or().andMovieIdEqualTo(movieId);
				List<MovieInfoWithBLOBs> movieInfoWithBLOBs = movieInfoMapper
						.selectByExampleWithBLOBs(movieInfoExample);
				if (movieInfoWithBLOBs != null) {
					movieInfos.addAll(movieInfoWithBLOBs);
				}
			}
			return new MyJson(true, movieInfos);
		} catch (Exception e) {
			System.err.println(e);
			return new MyJson(false, ErrInfoStrings.DATABASE_ERR);
		}
	}

	@Override
	public MyJson getMovieById(Integer movieId) {
		MovieInfoExample movieInfoExample = new MovieInfoExample();
		movieInfoExample.or().andMovieIdEqualTo(movieId);
		try {
			List<MovieInfoWithBLOBs> movieInfos = movieInfoMapper.selectByExampleWithBLOBs(movieInfoExample);
			if (movieInfos.size() != 0) {
				// 查到结果
				return new MyJson(true, movieInfos.get(0));
			}
			// 无结果，防止传回空指针
			return new MyJson(true, new MovieInfo());
		} catch (Exception e) {
			System.err.println(e);
			return new MyJson(false, ErrInfoStrings.DATABASE_ERR);
		}
	}

	@Override
	public MyJson getRecommendMovies() {
		try {
			List<MovieInfoWithBLOBs> movieInfoWithBLOBs = movieInfoMapper.randomRecommend();
			return new MyJson(true, movieInfoWithBLOBs);
		} catch (Exception e) {
			System.err.println(e);
			return new MyJson(false, ErrInfoStrings.DATABASE_ERR);
		}
	}

	@Override
	public MyJson getTimelyMovie() {
		TimelyMovieExample timelyMovieExample = new TimelyMovieExample();
		timelyMovieExample.or();
		try {
			return new MyJson(true, timelyMovieMapper.selectByExample(timelyMovieExample));
		} catch (Exception e) {
			System.err.println(e);
			return new MyJson(false, ErrInfoStrings.DATABASE_ERR);
		}
	}

	// TODO: 推荐算法实现
	@Override
	public MyJson getRecommendMovies(HttpSession session) {
		User user = (User) session.getAttribute("user");
		if (user == null) {
			return getRecommendMovies();
		}
		MovieInfoRecommendExample movieInfoRecommendExample = new MovieInfoRecommendExample();
		movieInfoRecommendExample.or().andUserIdEqualTo(user.getUserId());
		try {
			List<MovieInfoRecommendWithBLOBs> movieInfoRecommendWithBLOBs = movieInfoRecommendMapper
					.selectByExampleWithBLOBs(movieInfoRecommendExample);
			if (movieInfoRecommendWithBLOBs.size() == 0) {
				// 冷启动用户随机推荐
				return getRecommendMovies();
			}
			return new MyJson(true, movieInfoRecommendWithBLOBs);
		} catch (Exception e) {
			System.err.println(e);
			return new MyJson(false, ErrInfoStrings.DATABASE_ERR);
		}

	}

}

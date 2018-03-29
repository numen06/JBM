package service.test.event;

import java.util.Date;

import com.td.framework.event.bean.RemoteEventBean;

//@Service
public class WalkEvent extends RemoteEventBean {

	public WalkEvent(Object source) {
		super(source);
	}

	public WalkEvent(Object source, Date date) {
		super(source);
		this.times = date;
	}

	private static final long serialVersionUID = -2340647434825335283L;
	public Date times;

}
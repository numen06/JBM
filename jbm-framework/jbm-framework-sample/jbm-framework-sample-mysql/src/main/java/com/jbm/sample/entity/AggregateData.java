package com.jbm.sample.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.jbm.framework.masterdata.usage.bean.MasterDataEntity;

/**   
* @Description: 汇总数据
* @date 2017年5月10日 下午1:12:21 
* @version version1.0   
*/
@Entity
@Table(name="aggregate_data")
public class AggregateData extends MasterDataEntity{
	private static final long serialVersionUID = 1L;
	
	private Long projectId;
	private Long structureId;
	private Long energyTypeId;
	private Long time;
	private Date dateTime;

	private String dataType;
	private Double data;
	private Date updateTime;
	//水电气
	private Long energysBaseTypeId;
	//标准煤
	private Double standardCoal;

	public Double getStandardCoal() {
		return standardCoal;
	}

	public void setStandardCoal(Double standardCoal) {
		this.standardCoal = standardCoal;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public Long getEnergysBaseTypeId() {
		return energysBaseTypeId;
	}

	public void setEnergysBaseTypeId(Long energysBaseTypeId) {
		this.energysBaseTypeId = energysBaseTypeId;
	}

	public Long getProjectId() {
		return projectId;
	}
	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}
	public Long getStructureId() {
		return structureId;
	}
	public void setStructureId(Long structureId) {
		this.structureId = structureId;
	}

	public Date getDateTime() {
		return dateTime;
	}

	public void setDateTime(Date dateTime) {
		this.dateTime = dateTime;
	}

	public String getDataType() {
		return dataType;
	}
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	public Double getData() {
		return data;
	}
	public void setData(Double data) {
		this.data = data;
	}
	public Long getEnergyTypeId() {
		return energyTypeId;
	}
	public void setEnergyTypeId(Long energyTypeId) {
		this.energyTypeId = energyTypeId;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}
}

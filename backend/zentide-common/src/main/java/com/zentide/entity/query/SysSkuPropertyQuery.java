package com.zentide.entity.query;



/**
 * 参数
 */
public class SysSkuPropertyQuery extends BaseParam {


	/**
	 * 属性ID
	 */
	private String propertyId;

	private String propertyIdFuzzy;

	/**
	 * 属性名称
	 */
	private String propertyName;

	private String propertyNameFuzzy;

	/**
	 * 一级分类
	 */
	private String categoryId;

	private String categoryIdFuzzy;

	/**
	 * 二级分类
	 */
	private String pCategoryId;

	private String pCategoryIdFuzzy;

	/**
	 * 排序
	 */
	private Integer sort;


	public void setPropertyId(String propertyId){
		this.propertyId = propertyId;
	}

	public String getPropertyId(){
		return this.propertyId;
	}

	public void setPropertyIdFuzzy(String propertyIdFuzzy){
		this.propertyIdFuzzy = propertyIdFuzzy;
	}

	public String getPropertyIdFuzzy(){
		return this.propertyIdFuzzy;
	}

	public void setPropertyName(String propertyName){
		this.propertyName = propertyName;
	}

	public String getPropertyName(){
		return this.propertyName;
	}

	public void setPropertyNameFuzzy(String propertyNameFuzzy){
		this.propertyNameFuzzy = propertyNameFuzzy;
	}

	public String getPropertyNameFuzzy(){
		return this.propertyNameFuzzy;
	}

	public void setCategoryId(String categoryId){
		this.categoryId = categoryId;
	}

	public String getCategoryId(){
		return this.categoryId;
	}

	public void setCategoryIdFuzzy(String categoryIdFuzzy){
		this.categoryIdFuzzy = categoryIdFuzzy;
	}

	public String getCategoryIdFuzzy(){
		return this.categoryIdFuzzy;
	}

	public void setpCategoryId(String pCategoryId){
		this.pCategoryId = pCategoryId;
	}

	public String getpCategoryId(){
		return this.pCategoryId;
	}

	public void setpCategoryIdFuzzy(String pCategoryIdFuzzy){
		this.pCategoryIdFuzzy = pCategoryIdFuzzy;
	}

	public String getpCategoryIdFuzzy(){
		return this.pCategoryIdFuzzy;
	}

	public void setSort(Integer sort){
		this.sort = sort;
	}

	public Integer getSort(){
		return this.sort;
	}

}

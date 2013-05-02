/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/ 
package org.olat.core.gui.components.form.flexible.impl.elements.table;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.olat.core.CoreSpringFactory;
import org.olat.core.dispatcher.mapper.MapperService;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentEventListener;
import org.olat.core.gui.components.choice.Choice;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemCollection;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormItemImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.FormLinkImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.TextElementImpl;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.StringHelper;
import org.olat.core.util.ValidationStatus;


/**
 * 
 * @author Christian Guretzki
 */
public class FlexiTableElementImpl extends FormItemImpl implements FlexiTableElement, FormItemCollection,
	ControllerEventListener, ComponentEventListener {

	//settings
	private boolean multiSelect;
	private FlexiTableRendererType rendererType = FlexiTableRendererType.classic;
	
	private int rowCount = -1;
	
	private int currentPage;
	private int currentFirstResult;
	private int pageSize;
	private boolean searchField;
	private boolean selectAllEnabled;

	private FormLink customButton;
	private FormLink searchButton;
	private TextElement searchFieldEl;
	
	private final FlexiTableDataModel<?> dataModel;
	private FlexiTableDataSource<?> dataSource;
	private final FlexiTableComponent component;
	private CloseableCalloutWindowController callout;
	private final WindowControl wControl;
	private final String mapperUrl;
	
	private Object selectedObj;
	private boolean allSelectedIndex;
	private Set<Integer> multiSelectedIndex;
	private Set<Integer> enabledColumnIndex = new HashSet<Integer>();
	private Map<String,FormItem> components = new HashMap<String,FormItem>();
	
	public FlexiTableElementImpl(UserRequest ureq, WindowControl wControl, String name, FlexiTableDataModel<?> tableModel) {
		this(ureq, wControl, name, null, tableModel, -1, false);
	}
	
	public FlexiTableElementImpl(UserRequest ureq, WindowControl wControl, String name, Translator translator, FlexiTableDataModel<?> tableModel) {
		this(ureq, wControl, name, translator, tableModel, -1, false);
	}
	
	public FlexiTableElementImpl(UserRequest ureq, WindowControl wControl, String name, Translator translator,
			FlexiTableDataModel<?> tableModel, int pageSize, boolean searchField) {
		super(name);
		this.wControl = wControl;
		this.dataModel = tableModel;
		this.dataSource = (tableModel instanceof FlexiTableDataSource) ? (FlexiTableDataSource<?>)dataModel : null;
		component = new FlexiTableComponent(this, translator);
		
		for(int i=dataModel.getTableColumnModel().getColumnCount(); i-->0; ) {
			FlexiColumnModel col = dataModel.getTableColumnModel().getColumnModel(i);
			if(col.isDefaultVisible()) {
				enabledColumnIndex.add(new Integer(col.getColumnIndex()));
			}
		}

		MapperService mapper = CoreSpringFactory.getImpl(MapperService.class);
		mapperUrl = mapper.register(ureq.getUserSession(), new FlexiTableModelMapper(component));

		String dispatchId = component.getDispatchID();
		this.searchField = searchField;
		if(searchField) {
			searchFieldEl = new TextElementImpl(dispatchId + "_searchField", "search", "");
			searchFieldEl.showLabel(false);
			components.put("rSearch", searchFieldEl);
			searchButton = new FormLinkImpl(dispatchId + "_searchButton", "rSearchButton", "search", Link.BUTTON);
			searchButton.setTranslator(translator);
			searchButton.setCustomEnabledLinkCSS("b_with_small_icon_right b_with_small_icon_only o_fulltext_search_button");
			components.put("rSearchB", searchButton);
		}
		
		customButton = new FormLinkImpl(dispatchId + "_customButton", "rCustomButton", "search", Link.BUTTON);
		customButton.setTranslator(translator);
		customButton.setCustomEnabledLinkCSS("b_with_small_icon_right b_with_small_icon_only b_table_prefs");
		components.put("rCustomize", customButton);

		this.pageSize = pageSize;
		if(pageSize > 0) {
			setPage(0);
		}
		
		if(dataSource != null) {
			//preload it
			dataSource.load(0, pageSize);
		}
	}

	public FlexiTableRendererType getRendererType() {
		return rendererType;
	}
	
	public void setRendererType(FlexiTableRendererType rendererType) {
		this.rendererType = rendererType;
	}
	
	@Override
	public boolean isMultiSelect() {
		return multiSelect;
	}
	
	@Override
	public void setMultiSelect(boolean multiSelect) {
		this.multiSelect = multiSelect;
	}

	@Override
	public boolean isSearchEnabled() {
		return searchField;
	}
	
	@Override
	public boolean isSelectAllEnable() {
		return selectAllEnabled;
	}
	
	@Override
	public void setSelectAllEnable(boolean enable) {
		this.selectAllEnabled = enable;
	}

	public String getSearchText() {
		return searchFieldEl == null ? null : searchFieldEl.getValue();
	}

	public TextElement getSearchElement() {
		return searchFieldEl;
	}
	
	public FormItem getSearchButton() {
		return searchButton;
	}
	
	public FormItem getCustomButton() {
		return customButton;
	}

	public Object getSelectedObj() {
		return selectedObj;
	}

	public void setSelectedObj(Object selectedObj) {
		this.selectedObj = selectedObj;
	}

	@Override
	public int getPageSize() {
		return pageSize;
	}

	@Override
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	
	public int getPage() {
		return currentPage;
	}
	
	@Override
	public void setPage(int page) {
		currentPage = page;//TODO
		//tableModel.load(0, getPageSize());
	}

	public int getCurrentFirstResult() {
		return currentFirstResult;
	}

	public void setCurrentFirstResult(int currentFirstResult) {
		this.currentFirstResult = currentFirstResult;
	}

	public String getMapperUrl() {
		return mapperUrl;
	}

	@Override
	public Iterable<FormItem> getFormItems() {
		return components.values();
	}

	@Override
	public FormItem getFormComponent(String name) {
		return components.get(name);
	}
	
	protected void addFormItem(FormItem item) {
		components.put(item.getName(), item);
	}

	@Override
	public void doDispatchFormRequest(UserRequest ureq) {
		super.doDispatchFormRequest(ureq);
	}
	
	@Override
	protected void dispatchFormRequest(UserRequest ureq) {
		super.dispatchFormRequest(ureq);
	}
	
	/**
	 * @see org.olat.core.gui.components.form.flexible.FormItemImpl#evalFormRequest(org.olat.core.gui.UserRequest)
	 */
	@Override
	public void evalFormRequest(UserRequest ureq) {
		String[] selectedIndexArr = getRootForm().getRequestParameterValues("tb_ms");
		if(selectedIndexArr != null) {
			setMultiSelectIndex(selectedIndexArr);
		}

		String selectedIndex = getRootForm().getRequestParameter("rSelect");
		String dispatchuri = getRootForm().getRequestParameter("dispatchuri");
		String select = getRootForm().getRequestParameter("select");
		if("undefined".equals(dispatchuri)) {
			evalSearchRequest(ureq);
		} else if(StringHelper.containsNonWhitespace(selectedIndex)) {
			int index = selectedIndex.lastIndexOf('-');
			if(index > 0 && index+1 < selectedIndex.length()) {
				String pos = selectedIndex.substring(index+1);
				int selectedPosition = Integer.parseInt(pos);
				selectedObj = dataModel.getObject(selectedPosition);
				doSelect(ureq, selectedPosition);
			}
		} else if(searchButton != null
				&& searchButton.getFormDispatchId().equals(dispatchuri)) {
			evalSearchRequest(ureq);
		} else if(dispatchuri != null && select != null && select.equals("checkall")) {
			doSelectAll();
		} else if(dispatchuri != null && select != null && select.equals("uncheckall")) {
			doUnSelectAll();
		} else if(customButton != null
				&& customButton.getFormDispatchId().equals(dispatchuri)) {
			//snap the request
			customizeCallout(ureq);
		} else {
			FlexiTableColumnModel colModel = dataModel.getTableColumnModel();
			for(int i=colModel.getColumnCount(); i-->0; ) {
				FlexiColumnModel col = colModel.getColumnModel(i);
				if(col.getAction() != null) {
					String selectedRowIndex = getRootForm().getRequestParameter(col.getAction());
					if(StringHelper.containsNonWhitespace(selectedRowIndex)) {
						doSelect(ureq, col.getAction(), Integer.parseInt(selectedRowIndex));
					}
				}
			}
		}
	}
	
	@Override
	public void dispatchEvent(UserRequest ureq, Controller source, Event event) {
		if(source == callout) {
			//System.out.println("dispatchEvent (Controller): " + source);
		}
	}
	
	@Override
	public void dispatchEvent(UserRequest ureq, Component source, Event event) {
		if(source instanceof Choice) {
			Choice visibleColsChoice = (Choice)source;
			setCustomizedColumns(visibleColsChoice);
			callout.deactivate();
		}
	}

	protected void customizeCallout(UserRequest ureq) {
		Choice choice = getColumnListAndTheirVisibility();
		callout = new CloseableCalloutWindowController(ureq, wControl, choice,
				customButton, "Customize", true, "o_sel_flexi_custom_callout");
		callout.activate();
		callout.addControllerListener(this);
	}
	
	protected boolean isColumnModelVisible(FlexiColumnModel col) {
		return enabledColumnIndex.contains(col.getColumnIndex());
	}
	
	protected void setCustomizedColumns(Choice visibleColsChoice) {
		List<Integer> visibleCols = visibleColsChoice.getSelectedRows();
		if(visibleCols.size() > 1) {
			VisibleFlexiColumnsModel model = (VisibleFlexiColumnsModel)visibleColsChoice.getTableDataModel();
			for(int i=model.getRowCount(); i-->0; ) {
				FlexiColumnModel col = model.getObject(i);
				if(visibleCols.contains(new Integer(i))) {
					enabledColumnIndex.add(col.getColumnIndex());
				} else {
					enabledColumnIndex.remove(col.getColumnIndex());
				}
			}
		}
		component.setDirty(true);
	}
	
	private Choice getColumnListAndTheirVisibility() {
		Choice choice = new Choice("colchoice", getTranslator());
		choice.setTableDataModel(new VisibleFlexiColumnsModel(dataModel.getTableColumnModel(), enabledColumnIndex, getTranslator()));
		choice.addListener(this);
		choice.setCancelKey("cancel");
		choice.setSubmitKey("save");
		return choice;
	}

	protected void evalSearchRequest(UserRequest ureq) {
		if(searchFieldEl == null) return;//this a default behavior which can occur without the search configured
		searchFieldEl.evalFormRequest(ureq);
		String search = searchFieldEl.getValue();
		if(StringHelper.containsNonWhitespace(search)) {
			doSearch(ureq, search);
		} else {
			doResetSearch(ureq);
		}
	}
	
	protected void doSelectAll() {
		allSelectedIndex = true;
		if(multiSelectedIndex != null) {
			multiSelectedIndex.clear();
		}
	}
	
	protected void doUnSelectAll() {
		allSelectedIndex = false;
		if(multiSelectedIndex != null) {
			multiSelectedIndex.clear();
		}
	}
	
	protected void doSelect(UserRequest ureq, int index) {
		getRootForm().fireFormEvent(ureq, new SelectionEvent(ROM_SELECT_EVENT, index, this, FormEvent.ONCLICK));
	}
	
	protected void doSelect(UserRequest ureq, String action, int index) {
		getRootForm().fireFormEvent(ureq, new SelectionEvent(action, index, this, FormEvent.ONCLICK));
	}
	
	protected void doSearch(UserRequest ureq, String search) {
		if(dataSource != null) {
			resetInternComponents();
			dataSource.search(search, null, 0, getPageSize());
		}
	}
	
	protected void doResetSearch(UserRequest ureq) {
		if(dataSource != null) {
			resetInternComponents();
			dataSource.load(0, getPageSize());
		}
	}

	@Override
	public boolean isAllSelectedIndex() {
		return allSelectedIndex;
	}

	public void setAllSelectedIndex(boolean allSelectedIndex) {
		this.allSelectedIndex = allSelectedIndex;
	}

	@Override
	public Set<Integer> getMultiSelectedIndex() {
		if(allSelectedIndex && dataSource != null) {
			//ensure the whole data model is loaded
			dataSource.load(0, -1);
			Set<Integer> allIndex = new HashSet<Integer>();
			for(int i=dataModel.getRowCount(); i-->0; ) {
				allIndex.add(new Integer(i));
			}
			return allIndex;
		}
		return multiSelectedIndex == null ? Collections.<Integer>emptySet() : multiSelectedIndex;
	}

	@Override
	public boolean isMultiSelectedIndex(int index) {
		return allSelectedIndex
				|| (multiSelectedIndex != null && multiSelectedIndex.contains(new Integer(index)));
	}
	
	protected void setMultiSelectIndex(String[] selections) {
		if(multiSelectedIndex == null) {
			multiSelectedIndex = new HashSet<Integer>();
		}
		multiSelectedIndex.clear();
		//selection format row_{formDispId}-{index}
		if(selections.length > 0) {
			int index = selections[0].lastIndexOf('-');
			if(index > 0) {
				for(String selection:selections) {	
					if(index > 0 && index+1 < selection.length()) {
						String rowStr = selection.substring(index+1);
						int row = Integer.parseInt(rowStr);
						multiSelectedIndex.add(new Integer(row));
					}
				}
			}
		}
	}

	@Override
	public void validate(List<ValidationStatus> validationResults) {
		if(searchFieldEl != null) searchFieldEl.validate(validationResults);
		if(searchButton != null) searchButton.validate(validationResults);
		if(customButton != null) customButton.validate(validationResults);
	}

	@Override
	public void reset() {
		resetInternComponents();
		reloadData();
	}
	
	private void resetInternComponents() {
		rowCount = -1;
		component.setDirty(true);
		multiSelectedIndex = null;
	}

	@Override
	public void reloadData() {
		if(dataSource != null) {
			dataSource.load(0, getPageSize());//reload needed rows
		}
	}
	
	/**
	 * Prevent parent to be set as dirty for every request
	 */
	@Override
	public boolean isInlineEditingElement() {
		return true;
	}

	@Override
	protected void rootFormAvailable() {
		rootFormAvailable(searchFieldEl);
		rootFormAvailable(searchButton);
		rootFormAvailable(customButton);
	}
	
	private final void rootFormAvailable(FormItem item) {
		if(item != null && item.getRootForm() != getRootForm())
			item.setRootForm(getRootForm());
	}

	protected FlexiTableComponent getFormItemComponent() {
		return component;
	}
	
	public int getRowCount() {
		if(rowCount < 0) {
			rowCount = dataModel.getRowCount();
		}
		return rowCount;
	}
	
	public int getFirstRow() {
		return 0;
	}
	
	public int getMaxRows() {
		if(pageSize > 0) {
			return pageSize;
		}
		return getRowCount();
	}
	
	public FlexiTableDataModel<?> getTableDataModel() {
		return dataModel;
	}
	
	public FlexiTableDataSource<?> getTableDataSource() {
		return dataSource;
	}
}

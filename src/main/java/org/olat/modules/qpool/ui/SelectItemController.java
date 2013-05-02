/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.qpool.ui;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.segmentedview.SegmentViewComponent;
import org.olat.core.gui.components.segmentedview.SegmentViewEvent;
import org.olat.core.gui.components.segmentedview.SegmentViewFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.modules.qpool.ui.datasource.DefaultItemsSource;
import org.olat.modules.qpool.ui.events.QItemViewEvent;

/**
 * 
 * Initial date: 16.04.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class SelectItemController extends BasicController {
	
	private final Link markedItemsLink, ownedItemsLink;
	private final SegmentViewComponent segmentView;
	private final VelocityContainer mainVC;
	private ItemListController ownedItemsCtrl;
	private ItemListController markedItemsCtrl;
	private String restrictToFormat;
	
	public SelectItemController(UserRequest ureq, WindowControl wControl, String restrictToFormat) {
		super(ureq, wControl);
		this.restrictToFormat = restrictToFormat;
		mainVC = createVelocityContainer("item_list_overview");
		
		int marked = updateMarkedItems(ureq);
		if(marked <= 0) {
			updateOwnedGroups(ureq);
		}
		
		segmentView = SegmentViewFactory.createSegmentView("segments", mainVC, this);
		markedItemsLink = LinkFactory.createLink("menu.database.favorit", mainVC, this);
		segmentView.addSegment(markedItemsLink, marked > 0);
		ownedItemsLink = LinkFactory.createLink("menu.database.my", mainVC, this);
		segmentView.addSegment(ownedItemsLink, marked <= 0);
		putInitialPanel(mainVC);
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(source == segmentView) {
			if(event instanceof SegmentViewEvent) {
				SegmentViewEvent sve = (SegmentViewEvent)event;
				String segmentCName = sve.getComponentName();
				Component clickedLink = mainVC.getComponent(segmentCName);
				if (clickedLink == markedItemsLink) {
					updateMarkedItems(ureq);
				} else if (clickedLink == ownedItemsLink){
					updateOwnedGroups(ureq);
				}
			}
		}
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(event instanceof QItemViewEvent) {
			if("select-item".equals(event.getCommand())) {
				fireEvent(ureq, event);
			}	
		}
		super.event(ureq, source, event);
	}

	private int updateMarkedItems(UserRequest ureq) {
		if(markedItemsCtrl == null) {
			DefaultItemsSource source = new DefaultItemsSource(getIdentity(), ureq.getUserSession().getRoles(), "Fav");
			source.getDefaultParams().setFavoritOnly(true);
			source.getDefaultParams().setFormat(restrictToFormat);
			markedItemsCtrl = new ItemListController(ureq, getWindowControl(), source);
			listenTo(markedItemsCtrl);
		}
		int numOfMarkedItems = markedItemsCtrl.updateList();
		mainVC.put("itemList", markedItemsCtrl.getInitialComponent());
		return numOfMarkedItems;
	}
	
	private void updateOwnedGroups(UserRequest ureq) {
		if(ownedItemsCtrl == null) {
			DefaultItemsSource source = new DefaultItemsSource(getIdentity(), ureq.getUserSession().getRoles(), "My"); 
			source.getDefaultParams().setAuthor(getIdentity());
			source.getDefaultParams().setFormat(restrictToFormat);
			ownedItemsCtrl = new ItemListController(ureq, getWindowControl(), source);
			listenTo(ownedItemsCtrl);
		}
		ownedItemsCtrl.updateList();
		mainVC.put("itemList", ownedItemsCtrl.getInitialComponent());
	}




}

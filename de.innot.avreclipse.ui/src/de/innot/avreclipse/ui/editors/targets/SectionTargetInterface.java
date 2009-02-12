/*******************************************************************************
 * 
 * Copyright (c) 2009 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id$
 *     
 *******************************************************************************/

package de.innot.avreclipse.ui.editors.targets;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import de.innot.avreclipse.core.targets.IProgrammer;
import de.innot.avreclipse.core.targets.ITargetConfigConstants;
import de.innot.avreclipse.core.targets.ITargetConfiguration;
import de.innot.avreclipse.core.targets.ITargetConfigurationWorkingCopy;
import de.innot.avreclipse.core.targets.TCValidator;
import de.innot.avreclipse.core.targets.TargetInterface;
import de.innot.avreclipse.core.targets.TCValidator.Problem;

/**
 * FormPart to edit all settings for the current target interface.
 * <p>
 * This part is implemented as a section.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public class SectionTargetInterface extends AbstractTargetConfigurationEditorPart implements
		ITargetConfigConstants {

	/** The list of target configuration attributes that this part manages. */
	private final static String[]	PART_ATTRS			= new String[] { //
														//
			ATTR_JTAG_CLOCK, //
			ATTR_DAISYCHAIN_ENABLE, //
			ATTR_DAISYCHAIN_UB, //
			ATTR_DAISYCHAIN_UA, //
			ATTR_DAISYCHAIN_BB, //
			ATTR_DAISYCHAIN_BA							};

	/** The list of target configuration attributes that cause this part to refresh. */
	private final static String[]	PART_DEPENDS		= new String[] { //
														//
			ATTR_PROGRAMMER_ID, //
			ATTR_FCPU									};

	/** the client area of the Section created by the superclass. */
	private Composite				fSectionClient;

	private Label					fFreqText;

	private Composite				fWarningCompo;

	/** The composite that contains the four daisy chain setting controls. */
	private Composite				fDaisyChainCompo;

	/** Map of the daisy chain attributes to their respective text controls. */
	private Map<String, Text>		fDaisyChainTexts	= new HashMap<String, Text>(4);

	/** the array of possible clock frequencies for the current programmer. */
	private int[]					fClockValues;

	/*
	 * (non-Javadoc)
	 * @see de.innot.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getTitle()
	 */
	@Override
	protected String getTitle() {
		// This is just a placeholder dummy.
		// The real name will be set in the refreshSectionContent() method.
		return "Host Interface";
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * de.innot.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getDescription()
	 */
	@Override
	protected String getDescription() {
		return null; // TODO: add a description
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * de.innot.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getPartAttributes
	 * ()
	 */
	@Override
	public String[] getPartAttributes() {
		return PART_ATTRS;
	}

	/*
	 * (non-Javadoc)
	 * @seede.innot.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#
	 * getDependentAttributes()
	 */
	@Override
	String[] getDependentAttributes() {
		return PART_DEPENDS;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * de.innot.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getSectionStyle
	 * ()
	 */
	@Override
	int getSectionStyle() {
		return Section.TWISTIE | Section.SHORT_TITLE_BAR | Section.EXPANDED | Section.CLIENT_INDENT;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	public void createSectionContent(Composite parent, FormToolkit toolkit) {

		TableWrapLayout layout = new TableWrapLayout();
		layout.horizontalSpacing = 12;
		parent.setLayout(layout);

		fSectionClient = parent;

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	@Override
	public void refreshSectionContent() {

		final ITargetConfigurationWorkingCopy tcwc = getTargetConfiguration();

		// Get the required information from the target configuration
		String programmerid = tcwc.getAttribute(ATTR_PROGRAMMER_ID);
		IProgrammer programmer = tcwc.getProgrammer(programmerid);
		TargetInterface newTI = programmer.getTargetInterface();
		fClockValues = programmer.getTargetInterfaceClockFrequencies();

		//
		// Clear the old section content
		//

		// First remove all old errors/warnings.
		// The MessageManager does not like disposed controls, so we have to remove
		// all messages first.
		// The warnings which are still valid are regenerated when the respective sections are
		// generated.
		IMessageManager mmngr = getMessageManager();
		if (fFreqText != null && !fFreqText.isDisposed()) {
			mmngr.removeMessages(fFreqText);
		}
		for (Control textcontrol : fDaisyChainTexts.values()) {
			if (!textcontrol.isDisposed()) {
				mmngr.removeMessages(textcontrol);
			}
		}

		// then remove all old controls from the section
		Control[] children = fSectionClient.getChildren();
		for (Control child : children) {
			child.dispose();
		}

		// Finally reflow the form. Otherwise layout artifacts may remain behind.
		getManagedForm().reflow(true);

		//
		// redraw the complete section.
		//

		String title = MessageFormat.format("{0} Settings", newTI.toString());
		getControl().setText(title);

		// And rebuild the content
		FormToolkit toolkit = getManagedForm().getToolkit();

		Control section = null;

		// Add the BitClock section if the target configuration has some bitclock values.
		// The target configuration knows which programmers have a settable bitclock and
		// which have not.
		if (fClockValues.length != 0) {
			section = addClockSection(fSectionClient, toolkit);
			section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		}

		// Add the Daisy Chain section if the target interface is capable of daisy chaining.
		if (programmer.isDaisyChainCapable()) {
			section = addJTAGDaisyChainSection(fSectionClient, toolkit);
			section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		}

		// If the target interface has neither settable clocks nor is daisy chain capable, then add
		// a small dummy text telling the user that there is nothing to set.
		if (section == null) {
			// the selected target interface has no options
			Label label = toolkit.createLabel(fSectionClient,
					"The selected progrmmer has no user changeable settings for the "
							+ newTI.toString() + " target interface", SWT.WRAP);
			label.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * de.innot.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#refreshWarnings
	 * ()
	 */
	public void updateProblems() {
		validateBitClock();
		validateDaisyChain();
	}

	/**
	 * Add the bit bang delay setting section to the parent.
	 * <p>
	 * The Section contains the controls for the ATTR_JTAGCLOCK attribute.
	 * </p>
	 * <p>
	 * It is up to the caller to set the appropriate layout data on the returned
	 * <code>Section</code> control.
	 * </p>
	 * 
	 * @param parent
	 *            Composite to which the section is added.
	 * @param toolkit
	 *            FormToolkit to use for the new controls.
	 */
	private Section addClockSection(Composite parent, FormToolkit toolkit) {

		//
		// The Section
		//

		Section section = toolkit.createSection(parent, Section.TWISTIE | Section.CLIENT_INDENT);

		section.setText("Clock Frequency");
		String desc = "The clock frequency must not be higher that 1/4 of "
				+ "the target MCU clock frequency. The default value depends on the "
				+ "selected tool, but is usually 1 MHz, suitable for target MCUs running "
				+ "at 4 MHz or above.";

		int jtagclock = getTargetConfiguration().getIntegerAttribute(ATTR_JTAG_CLOCK);
		// Collapse the section if the current value is 0 (= default) to reduce clutter
		section.setExpanded(jtagclock != 0);

		//
		// The Section content
		//
		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new TableWrapLayout());

		//
		// The description Label
		//
		Label description = toolkit.createLabel(sectionClient, desc, SWT.WRAP);
		description.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		// 
		// The actual controls, wrapped in a Composite with a 2 column GridLayout
		//
		Composite content = toolkit.createComposite(sectionClient);
		content.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = 12;
		content.setLayout(gl);

		final Scale scale = new Scale(content, SWT.HORIZONTAL);
		toolkit.adapt(scale, true, true);
		scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		scale.addSelectionListener(new SelectionAdapter() {

			/*
			 * (non-Javadoc)
			 * @seeorg.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.
			 * SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = scale.getSelection();
				int value = fClockValues[index];
				updateBitClockValue(value);
			}

		});

		// Set the scale properties.
		// we use an indirection: The scale does not set the Hz value directly.
		// instead it just selects the value from the fClockValues array.
		// The pageIncrements determine the number of ticks on the scale.
		// For up to 100 values in the bitclocks array we use 1 tick for each value.
		// For more than 100 values we use 1 tick for every 2 values.
		int units = fClockValues.length;
		scale.setMaximum(units - 1);
		scale.setMinimum(0);
		scale.setIncrement(1);
		scale.setPageIncrement(units < 100 ? 1 : 2);

		//
		// The frequency display.
		//
		fFreqText = toolkit.createLabel(content, "default", SWT.RIGHT);

		GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.widthHint = calcTextWidth(fFreqText, "8.888 MHz");
		fFreqText.setLayoutData(gd);

		//
		// The BitClock > 1/4 FCPU warning display
		//
		fWarningCompo = toolkit.createComposite(content);
		fWarningCompo.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
		fWarningCompo.setLayout(new GridLayout(2, false));
		Label image = toolkit.createLabel(fWarningCompo, "");
		image.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJS_WARN_TSK));
		image.setLayoutData(new GridData(SWT.BEGINNING, SWT.NONE, false, false));

		Label warning = toolkit.createLabel(fWarningCompo,
				"The selected BitClock Frequency is greater than 1/4th of the target MCU Clock");
		warning.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		fWarningCompo.setVisible(false);

		// Finally set the scale and the label to the current setting (or the next lower if a
		// different ClockValues table is used)
		int value = getTargetConfiguration().getIntegerAttribute(ATTR_JTAG_CLOCK);

		// find next lower value
		int lastv = 0;
		int index = 0;
		for (; index < fClockValues.length; index++) {
			if (fClockValues[index] <= value) {
				lastv = fClockValues[index];
			} else {
				break;
			}
		}

		scale.setSelection(index - 1);

		// Update the value. This will in turn set the bitclock warning if required.
		updateBitClockValue(lastv);

		// Now tell the section about its content.
		section.setClient(sectionClient);

		return section;
	}

	/**
	 * Updates the bitclock attribute in the target configuration and validates it.
	 * 
	 * @param value
	 *            The new bitclock frequency.
	 */
	private void updateBitClockValue(int value) {

		// Set the attribute
		getTargetConfiguration().setIntegerAttribute(ATTR_JTAG_CLOCK, value);
		getManagedForm().dirtyStateChanged();

		// update the frequency display label

		fFreqText.setText(convertFrequencyToString(value));

		validateBitClock();

	}

	/**
	 * Convert a integer Hz value to a String.
	 * <p>
	 * The result has the unit appended:
	 * <ul>
	 * <li><code>Hz</code> for values below 1KHZ</li>
	 * <li><code>KHz</code> for values between 1 and 1000 KHz</li>
	 * <li><code>MHz</code> for values above 1000 KHz</li>
	 * </ul>
	 * As a special case the value <code>0</code> will result in "default".
	 * </p>
	 * 
	 * @param value
	 *            integer Hz value
	 * @return
	 */
	private String convertFrequencyToString(int value) {
		String text;
		if (value == 0) {
			text = "default";
		} else if (value < 1000) {
			text = value + " Hz";
		} else if (value < 1000000) {
			float newvalue = value / 1000.0F;
			text = newvalue + " KHz";
		} else {
			float newvalue = value / 1000000.0F;
			text = newvalue + " MHz";
		}
		return text;
	}

	/**
	 * Show or hide the 1/4th MCU frequency warning.
	 * 
	 * @see TCValidator#checkJTAGClock(ITargetConfiguration)
	 */
	private void validateBitClock() {
		if (TCValidator.checkJTAGClock(getTargetConfiguration()).equals(Problem.WARN)) {
			// Set the warning compo visible and add a warning to the
			// MessageManager.
			String bitclock = getTargetConfiguration().getAttribute(ATTR_JTAG_CLOCK);
			int value = Integer.parseInt(bitclock);
			int targetfcpu = getTargetConfiguration().getFCPU();

			if (fWarningCompo != null && !fWarningCompo.isDisposed()) {
				fWarningCompo.setVisible(true);
			}

			String msg = MessageFormat
					.format(
							"selected BitClock Frequency of {0} is greater than 1/4th of the target MCU Clock ({1})",
							convertFrequencyToString(value), convertFrequencyToString(targetfcpu));
			getMessageManager().addMessage(ATTR_JTAG_CLOCK, msg, ATTR_JTAG_CLOCK,
					IMessageProvider.WARNING, fFreqText);

		} else {

			// No warning required. Remove the warning from the MessageManager (which is save even
			// if there was no warning) and hide the warning compo.
			// If the user has just changed to a different interface then the fWarningCompo will
			// already be disposed, so we need to check this.
			if (fFreqText != null && !fFreqText.isDisposed()) {
				getMessageManager().removeMessage(ATTR_JTAG_CLOCK, fFreqText);
			}
			if (fWarningCompo != null && !fWarningCompo.isDisposed()) {
				fWarningCompo.setVisible(false);
			}
		}
	}

	/**
	 * Add the JTAG daisy chain settings section to the parent.
	 * <p>
	 * The Section contains the controls for the ATTR_DAISYCHAIN_ENABLE and the four DAISYCHAIN_xx
	 * attributes.
	 * </p>
	 * <p>
	 * It is up to the caller to set the appropriate layout data on the returned
	 * <code>Section</code> control.
	 * </p>
	 * 
	 * @param parent
	 *            Composite to which the section is added.
	 * @param toolkit
	 *            FormToolkit to use for the new controls.
	 */
	private Section addJTAGDaisyChainSection(Composite parent, FormToolkit toolkit) {

		//
		// The Section
		//
		Section section = toolkit.createSection(parent, Section.TWISTIE | Section.CLIENT_INDENT);
		section.setText("Daisy Chain");
		String desc = "These settings are required if the target MCU is part of a JTAG daisy chain.\n"
				+ "Set the number of devices before and after the target MCU in the chain "
				+ "and the accumulated number of instruction bits they use. AVR devices use "
				+ "4 instruction bits, but other JTAG devices may differ. \n"
				+ "Note: JTAG daisy chains are only supported by some Programmers.";

		String enabledtext = getTargetConfiguration().getAttribute(ATTR_DAISYCHAIN_ENABLE);
		boolean enabled = Boolean.parseBoolean(enabledtext);

		// Collapse the section if Daisy chain is not enables to avoid clutter
		section.setExpanded(enabled);

		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new TableWrapLayout());

		//
		// The section description label
		//
		Label description = toolkit.createLabel(sectionClient, desc, SWT.WRAP);
		description.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		//
		// The Daisy Chain enable check button
		//
		boolean useDaisyChain = getTargetConfiguration()
				.getBooleanAttribute(ATTR_DAISYCHAIN_ENABLE);
		final Button enableButton = toolkit.createButton(sectionClient, "Enable daisy chain",
				SWT.CHECK);
		enableButton.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		enableButton.setSelection(useDaisyChain);
		enableButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean isEnabled = enableButton.getSelection();
				setEnabled(fDaisyChainCompo, isEnabled);
				getTargetConfiguration().setBooleanAttribute(ATTR_DAISYCHAIN_ENABLE, isEnabled);
				getManagedForm().dirtyStateChanged();
				validateDaisyChain();
			}
		});

		// 
		// The actual daisy chain controls, wrapped in a Composite with a 4 column GridLayout
		//

		fDaisyChainCompo = toolkit.createComposite(sectionClient);
		fDaisyChainCompo.setLayoutData(new TableWrapData(TableWrapData.FILL));
		GridLayout layout = new GridLayout(4, false);
		layout.horizontalSpacing = 12;
		fDaisyChainCompo.setLayout(layout);

		createDCTextField(fDaisyChainCompo, "Devices before:", ATTR_DAISYCHAIN_UB);
		createDCTextField(fDaisyChainCompo, "Instruction bits before:", ATTR_DAISYCHAIN_BB);

		createDCTextField(fDaisyChainCompo, "Devices after:", ATTR_DAISYCHAIN_UA);
		createDCTextField(fDaisyChainCompo, "Instruction bits after:", ATTR_DAISYCHAIN_BA);

		setEnabled(fDaisyChainCompo, useDaisyChain);

		section.setClient(sectionClient);

		// Once we have created the controls we can validate the target configuration to set any
		// problem markers.
		validateDaisyChain();

		return section;
	}

	/**
	 * Create a single daisy chain settings text control with a label.
	 * <p>
	 * The created text control is added to the {@link #fDaisyChainTexts} map with the given
	 * attribute as the key.
	 * </p>
	 * 
	 * @param parent
	 *            The parent composite (with a GridLayout)
	 * @param labeltext
	 *            The text for the label
	 * @param attribute
	 *            The target configuration attribute.
	 */
	private void createDCTextField(Composite parent, String labeltext, String attribute) {

		FormToolkit toolkit = getManagedForm().getToolkit();

		final ModifyListener modifylistener = new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				// Get the Attribute of the text field and its value
				String attr = (String) e.widget.getData();
				String value = ((Text) e.widget).getText();
				if (value.length() == 0) {
					value = "0";
				}

				int intvalue = Integer.parseInt(value);
				getTargetConfiguration().setIntegerAttribute(attr, intvalue);
				getManagedForm().dirtyStateChanged();
				validateDaisyChain();
			}
		};

		// The verify listener to restrict the input to integers
		final VerifyListener verifylistener = new VerifyListener() {
			public void verifyText(VerifyEvent event) {
				String text = event.text;
				if (!text.matches("[0-9]*")) {
					event.doit = false;
				}
			}
		};

		toolkit.createLabel(parent, labeltext);

		int currvalue = getTargetConfiguration().getIntegerAttribute(attribute);
		String currvaluestring = Integer.toString(currvalue);
		Text text = toolkit.createText(parent, currvaluestring, SWT.RIGHT);
		GridData gd = new GridData(SWT.FILL, SWT.NONE, false, false);
		gd.widthHint = calcTextWidth(text, "8888");
		text.setLayoutData(gd);
		text.setTextLimit(3);
		text.setData(attribute); // set the attribute for the modify listener
		text.addModifyListener(modifylistener);
		text.addVerifyListener(verifylistener);

		fDaisyChainTexts.put(attribute, text);

	}

	/**
	 * Add or remove the error messages for the daisy chain settings.
	 * 
	 * @see TCValidator#checkJTAGDaisyChainUnitsBefore(ITargetConfiguration)
	 * @see TCValidator#checkJTAGDaisyChainUnitsAfter(ITargetConfiguration)
	 * @see TCValidator#checkJTAGDaisyChainBitsBefore(ITargetConfiguration)
	 * @see TCValidator#checkJTAGDaisyChainBitsAfter(ITargetConfiguration)
	 */
	private void validateDaisyChain() {

		IMessageManager mmngr = getMessageManager();

		ITargetConfiguration config = getTargetConfiguration();

		//
		// Bits Before
		//
		Text textctrl = fDaisyChainTexts.get(ATTR_DAISYCHAIN_BB);
		if (textctrl != null && !textctrl.isDisposed()) {
			if (TCValidator.checkJTAGDaisyChainBitsBefore(config).equals(Problem.OK)) {
				mmngr.removeMessage(ATTR_DAISYCHAIN_BB, textctrl);
			} else {
				mmngr.addMessage(ATTR_DAISYCHAIN_BB,
						"Daisy chain 'bits before' out of range (0 - 255)", ATTR_DAISYCHAIN_BB,
						IMessageProvider.ERROR, textctrl);
			}
		}

		//
		// Bits After
		//
		textctrl = fDaisyChainTexts.get(ATTR_DAISYCHAIN_BA);
		if (textctrl != null && !textctrl.isDisposed()) {
			if (TCValidator.checkJTAGDaisyChainBitsAfter(config).equals(Problem.OK)) {
				mmngr.removeMessage(ATTR_DAISYCHAIN_BA, textctrl);
			} else {
				mmngr.addMessage(ATTR_DAISYCHAIN_BA,
						"Daisy chain 'bits after' out of range (0 - 255)", ATTR_DAISYCHAIN_BA,
						IMessageProvider.ERROR, textctrl);
			}
		}

		//
		// Units Before
		//
		textctrl = fDaisyChainTexts.get(ATTR_DAISYCHAIN_UB);
		if (textctrl != null && !textctrl.isDisposed()) {
			if (TCValidator.checkJTAGDaisyChainUnitsBefore(config).equals(Problem.OK)) {
				mmngr.removeMessage(ATTR_DAISYCHAIN_UB, textctrl);
			} else {
				mmngr.addMessage(ATTR_DAISYCHAIN_UB,
						"Daisy chain 'Devices before' greater than 'bits before'",
						ATTR_DAISYCHAIN_UB, IMessageProvider.ERROR, textctrl);
			}
		}

		//
		// Units After
		//
		textctrl = fDaisyChainTexts.get(ATTR_DAISYCHAIN_UA);
		if (textctrl != null && !textctrl.isDisposed()) {
			if (TCValidator.checkJTAGDaisyChainUnitsAfter(config).equals(Problem.OK)) {
				mmngr.removeMessage(ATTR_DAISYCHAIN_UA, textctrl);
			} else {
				mmngr.addMessage(ATTR_DAISYCHAIN_UA,
						"Daisy chain 'Devices after' greater than 'bits after'",
						ATTR_DAISYCHAIN_UA, IMessageProvider.ERROR, textctrl);
			}
		}

	}
}

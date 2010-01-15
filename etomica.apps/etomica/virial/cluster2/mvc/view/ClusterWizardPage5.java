package etomica.virial.cluster2.mvc.view;

import java.awt.Component;
import java.awt.Font;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import etomica.virial.cluster2.mvc.WizardController;
import etomica.virial.cluster2.mvc.WizardState;

import static etomica.virial.cluster2.mvc.view.ClusterWizardState.*;

public class ClusterWizardPage5 extends ClusterWizardPageTemplate {

  public ClusterWizardPage5(WizardController controller) {

    super(controller);
  }

  @Override
  public void attachDone() {

    ((JButton) getController().getState().getProperty(ClusterWizard.KEY_HELP_BUTTON)).setEnabled(false);
    ((JButton) getController().getState().getProperty(ClusterWizard.KEY_NEXT_BUTTON)).setEnabled(false);
    super.attachDone();
  }

  @Override
  public void detachDone() {

    ((JButton) getController().getState().getProperty(ClusterWizard.KEY_HELP_BUTTON)).setEnabled(true);
    ((JButton) getController().getState().getProperty(ClusterWizard.KEY_NEXT_BUTTON)).setEnabled(true);
    super.detachDone();
  }

  @Override
  public int getPageId() {

    return 5;
  }

  @Override
  protected JComponent createControls() {

    FormLayout layout = new FormLayout("250dlu", "pref, 10dlu:grow, pref");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder.setBorder(new EmptyBorder(0, 0, 0, 0));
    builder.setOpaque(false);
    // section
    builder.addSeparator("Summary and Generation Plan", new CellConstraints(1, 1));
    builder.add(summarySection(), new CellConstraints(1, 3));
    return builder.getPanel();
  }

  protected Component summarySection() {

    FormLayout layout = new FormLayout("10dlu, 230dlu", "220dlu:grow");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder.setBorder(new EmptyBorder(0, 0, 0, 0));
    builder.setOpaque(false);
    builder.add(createSummary(), new CellConstraints(2, 1));
    return builder.getPanel();
  }

  protected JComponent createSummary() {

    JTextArea summary = buildTextArea(getSummaryText(), false, true);
    summary.setOpaque(true);
    summary.setBackground(ApplicationUI.uiSettings.getSelectedTheme().getPrimaryControl());
    summary.setFont(new Font("Monospaced", summary.getFont().getStyle(), summary.getFont().getSize() - 1));
    summary.setCaretPosition(0);
    JScrollPane pane = new JScrollPane(summary);
    pane.setOpaque(false);
    pane.getViewport().setOpaque(false);

    return pane;
  }

  @SuppressWarnings("unchecked")
  private String getSummaryText() {

    WizardState state = getController().getState();
    int totalNodes = (Integer) state.getProperty(KEY_TOTAL_NODES);
    int rootNodes = (Integer) state.getProperty(KEY_ROOT_NODES);
    boolean classAny = (Boolean) state.getProperty(KEY_CLASS_ANY);
    boolean classConnected = (Boolean) state.getProperty(KEY_CLASS_CONNECTED);
    boolean classBiconnected = (Boolean) state.getProperty(KEY_CLASS_BICONNECTED);
    boolean classReeHoover = (Boolean) state.getProperty(KEY_CLASS_REEHOOVER);
    String connectivityClass = classAny ? "all clusters" : classConnected ? "connected clusters"
        : classBiconnected ? "biconnected clusters" : classReeHoover ? "Ree-Hoover clusters" : "unknown";
    String result = "";
    result += "Cluster Specification Summary\n";
    result += "-----------------------------\n\n";
    result += "1. Global Properties\n\n";
    result += String.format("   Cluster Name...........: %s\n", state.getProperty(KEY_NAME));
    result += String.format("   Total Nodes............: %d\n", totalNodes);
    result += String.format("   Root Nodes.............: %d\n", rootNodes);
    result += String.format("   Field Nodes............: %d\n", totalNodes - rootNodes);
    result += String.format("   Color Scheme...........: %s\n", state.getProperty(KEY_COLOR_SCHEME));
    result += String.format("   Isomorph-Free..........: %s\n\n", state.getProperty(KEY_ISOMORPH_FREE));
    result += "2. Connectivity Properties\n\n";
    result += String.format("   Connectivity Class............: %s\n", connectivityClass);
    result += String.format("   Exclude Nodal Points..........: %s\n", state
        .getProperty(KEY_EXCLUDE_NODAL_POINTS));
    result += String.format("   Exclude Articulation Points...: %s\n", state
        .getProperty(KEY_EXCLUDE_ARTICULATION_POINTS));
    result += String.format("   Exclude Articulation Pairs....: %s\n\n", state
        .getProperty(KEY_EXCLUDE_ARTICULATION_PAIRS));
    result += "3. Node Colors\n\n";
    int colorIndex = 0;
    for (int i = 0; i < rootNodes; i++) {
      ColorEntry entry = (ColorEntry) state.getProperty(KEY_NODE_COLORS.get(colorIndex));
      result += String.format("   Root Node %d........: %s\n", i, entry.getText());
      colorIndex++;
    }
    for (int i = 0; i < totalNodes - rootNodes; i++) {
      ColorEntry entry = (ColorEntry) state.getProperty(KEY_NODE_COLORS.get(colorIndex));
      result += String.format("   Field Node %d.......: %s\n", i, entry.getText());
      colorIndex++;
    }
    // plan information
    List<ColorEntry> colorAssignment = (List<ColorEntry>) state.getProperty(KEY_ASSIGNED_COLORS);
    boolean effectivelyMono = state.getProperty(KEY_COLOR_SCHEME).equals(DEFVAL_MONOCHROMATIC)
        || colorAssignment.size() == 1;
    result += "\nCluster Generation Plan\n";
    result += "-----------------------\n\n";
    result += String.format("   ==> %d distinct colors assigned\n", colorAssignment.size());
    result += String.format("   ==> effectively %s\n", effectivelyMono ? DEFVAL_MONOCHROMATIC
        : DEFVAL_MULTICOLORED);
    if ((Boolean) state.getProperty(KEY_ISOMORPH_FREE)) {
      result += "   ==> isomorph-free generation\n";
    }

    int cost = 0;

    // cluster size and isomorphism
    if ((Boolean) state.getProperty(KEY_ISOMORPH_FREE)) {
      if (totalNodes > 9) {
        cost += 750 * totalNodes * totalNodes * totalNodes * totalNodes;
        result += String.format("   ==> %d total nodes, so using the naïve cluster generator\n", totalNodes);
        result += String.format("       updated cost: %d\n", cost);
      }
      else {
        cost += 5 * totalNodes;
        result += "   ==> using clusters precomputed by nauty\n";
        result += String.format("       updated cost: %d\n", cost);
      }
    }
    else {
      if (totalNodes > 7) {
        cost += 1250 * totalNodes * totalNodes * totalNodes;
        result += "   ==> using the naïve cluster generator\n";
        result += String.format("       updated cost: %d\n", cost);
      }
      else if (totalNodes > 6) {
        cost += 750 * totalNodes * totalNodes * totalNodes;
        result += "   ==> using the naïve cluster generator\n";
        result += String.format("       updated cost: %d\n", cost);
      }
      else {
        cost += 150 * totalNodes * totalNodes * totalNodes;
        result += "   ==> using the naïve cluster generator\n";
        result += String.format("       updated cost: %d\n", cost);
      }
    }

    // multicolored permutation cost
    if (!effectivelyMono) {
      cost *= 5 * cost;
      result += "   ==> running the naïve color permutator\n";
      result += String.format("       updated cost: %d\n", cost);
    }

    // connectivity cost: by class
    if (classConnected) {
      cost += 5 * totalNodes;
      result += "   ==> running the connectivity filter\n";
      result += String.format("       updated cost: %d\n", cost);
    }
    if (classBiconnected) {
      cost += 30 * totalNodes;
      result += "   ==> running the biconnectivity filter\n";
      result += String.format("       updated cost: %d\n", cost);
    }
    if (classReeHoover) {
      cost += 50 * totalNodes;
      result += "   ==> running the Ree-Hoover filter\n";
      result += String.format("       updated cost: %d\n", cost);
    }

    // additional connectivity cost
    if ((Boolean) state.getProperty(KEY_EXCLUDE_NODAL_POINTS)) {
      cost += 50 * totalNodes;
      result += "   ==> running the nodal points filter\n";
      result += String.format("       updated cost: %d\n", cost);
    }
    if ((Boolean) state.getProperty(KEY_EXCLUDE_ARTICULATION_POINTS)) {
      cost += 125 * totalNodes * totalNodes;
      result += "   ==> running the articulation points filter\n";
      result += String.format("       updated cost: %d\n", cost);
    }
    if ((Boolean) state.getProperty(KEY_EXCLUDE_ARTICULATION_PAIRS)) {
      cost += 375 * totalNodes * totalNodes * totalNodes;
      result += "   ==> running the articulation pairs filter\n";
      result += String.format("       updated cost: %d\n", cost);
    }

    return result;
  }

  @Override
  protected String getTitle() {

    String title = "Check that the cluster specifications are correct before you start the generation. ";
    title += "Please go over the generation plan to have a rough idea of how the generation is setup ";
    title += "and, therefore, how long it may take to complete.";
    return title;
  }
}
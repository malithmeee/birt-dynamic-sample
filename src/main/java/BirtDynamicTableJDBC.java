import com.ibm.icu.util.ULocale;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.model.api.*;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.elements.structures.ComputedColumn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic Table BIRT Design Engine API (DEAPI) demo using JDBC mysql connection.
 */
public class BirtDynamicTableJDBC {

    ReportDesignHandle designHandle = null;
    ElementFactory designFactory = null;
    StructureFactory structFactory = null;

    public static void main(String[] args) {
        try {
            BirtDynamicTableJDBC de = new BirtDynamicTableJDBC();
            /**
             * In my case, I have created database call local_db and table call localization including the following
             * fields. Add more fields than followings.
             */
            List<String> al = new ArrayList<String>();
            al.add("local_id");
            al.add("local_description");
            al.add("local_key");
            de.buildReport((ArrayList) al, "From localization");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SemanticException e) {
            e.printStackTrace();
        }
    }

    void buildDataSource() throws SemanticException {
        OdaDataSourceHandle dsHandle = designFactory.newOdaDataSource("elg_report", "org.eclipse.birt.report.data.oda.jdbc");
        dsHandle.setProperty("odaDriverClass", "com.mysql.jdbc.Driver");
        dsHandle.setProperty("odaURL", "jdbc:mysql://localhost:3306/local_db?createDatabaseIfNotExist=true");
        dsHandle.setProperty("odaUser", "root");
        dsHandle.setProperty("odaPassword", "password");
        designHandle.getDataSources().add(dsHandle);
    }

    void buildDataSet(ArrayList cols, String fromClause) throws SemanticException {
        OdaDataSetHandle dsHandle = designFactory.newOdaDataSet("ds", "org.eclipse.birt.report.data.oda.jdbc.JdbcSelectDataSet");
        dsHandle.setDataSource("elg_report");
        String qry = "Select ";
        for (int i = 0; i < cols.size(); i++) {
            qry += " " + cols.get(i);
            if (i != (cols.size() - 1)) {
                qry += ",";
            }
        }
        qry += " " + fromClause;
        dsHandle.setQueryText(qry);
        designHandle.getDataSets().add(dsHandle);
    }

    void buildReport(ArrayList cols, String fromClause) throws IOException, SemanticException {
        //Configure the Engine and start the Platform
        DesignConfig config = new DesignConfig();
        // When using maven dependency for birt runtime, it's no need to add birt runtime path like below
        config.setProperty("BIRT_HOME", "/home/malith/usr/eclips/birt-runtime-4_2_2/");
        IDesignEngine engine = null;
        try {
            Platform.startup(config);
            IDesignEngineFactory factory = (IDesignEngineFactory) Platform.createFactoryObject(IDesignEngineFactory.EXTENSION_DESIGN_ENGINE_FACTORY);
            engine = factory.createDesignEngine(config);
        } catch (Exception ex) {
            engine = new DesignEngine(config);
            ex.printStackTrace();
        }
        SessionHandle session = engine.newSessionHandle(ULocale.ENGLISH);
        try {
            /**
             * Open a design or a template which already created
             * Change the following path according to your local machine environment
             */
            designHandle = session.openDesign("/home/malith/Projects/elg/training/samples/birt-dynamic/rptdesigns/test.rptdesign");
            designFactory = designHandle.getElementFactory();
            buildDataSource();
            buildDataSet(cols, fromClause);
            TableHandle table = designFactory.newTableItem("table", cols.size());
            table.setWidth("100%");
            table.setDataSet(designHandle.findDataSet("ds"));
            PropertyHandle computedSet = table.getColumnBindings();
            ComputedColumn cs1 = null;
            for (int i = 0; i < cols.size(); i++) {
                cs1 = StructureFactory.createComputedColumn();
                cs1.setName((String) cols.get(i));
                cs1.setExpression("dataSetRow[\"" + (String) cols.get(i) + "\"]");
                computedSet.addItem(cs1);
            }
            // table header
            RowHandle tableheader = (RowHandle) table.getHeader().get(0);
            for (int i = 0; i < cols.size(); i++) {
                LabelHandle label1 = designFactory.newLabel((String) cols.get(i));
                label1.setText((String) cols.get(i));
                CellHandle cell = (CellHandle) tableheader.getCells().get(i);
                cell.getContent().add(label1);
            }
            // table detail
            RowHandle tabledetail = (RowHandle) table.getDetail().get(0);
            for (int i = 0; i < cols.size(); i++) {
                CellHandle cell = (CellHandle) tabledetail.getCells().get(i);
                DataItemHandle data = designFactory.newDataItem("data_" + (String) cols.get(i));
                data.setResultSetColumn((String) cols.get(i));
                cell.getContent().add(data);
            }
            designHandle.getBody().add(table);
            /**Save the design and closeit
             * Change the following path according to your local machine environment
             */
            designHandle.saveAs("/home/malith/Projects/elg/training/samples/birt-dynamic/rptdesigns/sample_jdbc.rptdesign"); //$NON-NLS-1$
            designHandle.close();
            System.out.println("Finished JDBC");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
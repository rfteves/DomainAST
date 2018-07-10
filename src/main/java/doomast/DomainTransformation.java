package doomast;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.ast.tools.GeneralUtils;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import java.lang.reflect.Modifier;
import java.net.URI;

@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
public class DomainTransformation extends AbstractASTTransformation {

    @Override
    public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
        if (IsValid(sourceUnit)) {
            addDateRangeInnerClass(sourceUnit);
            sourceUnit.getAST().getClasses().stream()
                    .forEach(classNode -> {
                        visit(classNode);
                        displayInterfaces(classNode);
                    });
        }
    }

    static private boolean IsValid(SourceUnit sourceUnit) {

        URI sourceURI = sourceUnit.getSource().getURI();

        if (!IsValidURIPath(sourceURI.getPath()))
            return false;

        return true;
    }

    static private boolean IsValidURIPath(String path) {
        if (path == null)
            return false;

        if (!path.endsWith(".groovy"))
            return false;

        if (!path.contains("domain/pr/Account"))
            return false;

        System.out.println("path is " + path);
        return true;
    }

    /*
        static getById(Long id) {
            def record = Domain.read(id)
            if (!record) {
                throw new RecordNotFoundException("Domain", "read", id)
            }
            record
        }
    */
    static private void visit(ClassNode classNode) {

        Parameter idParameter = new Parameter(
                ClassHelper.make(Long.class),
                "id"
        );

        VariableExpression idVariableExpression = new VariableExpression(idParameter);
        VariableExpression recordVariableExpression = new VariableExpression("record");

        BlockStatement methodBlockStatement = new BlockStatement();
        methodBlockStatement.addStatement(RecordReadStatement(classNode, recordVariableExpression, idVariableExpression));
        methodBlockStatement.addStatement(IfNotRecordStatement(classNode, recordVariableExpression, idVariableExpression));
        methodBlockStatement.addStatement(new ExpressionStatement(recordVariableExpression));


        classNode.addMethod(
                "getById",
                Modifier.STATIC,
                ClassHelper.OBJECT_TYPE,
                new Parameter[]{idParameter},
                new ClassNode[0],
                methodBlockStatement
        );
    }

    /*
        def record = Domain.read(id)
     */
    static private Statement RecordReadStatement(ClassNode domainClassNode, VariableExpression recordVariableExpression, Expression idExpression) {

        MethodCallExpression domainRead = new MethodCallExpression(
                new ClassExpression(domainClassNode),
                "read",
                idExpression
        );

        DeclarationExpression recordDeclaration = new DeclarationExpression(
                recordVariableExpression,
                GeneralUtils.ASSIGN,
                domainRead);

        return new ExpressionStatement(recordDeclaration);
    }

    /*
        if (!record) {
            throw new RecordNotFoundException("Domain, "read", id)
        }
    */
    static private IfStatement IfNotRecordStatement(ClassNode domainClassNode, Expression recordExpression, Expression idExpression) {

        BooleanExpression notRecord = new BooleanExpression(
                new NotExpression(recordExpression)
        );

        BlockStatement throwException = new BlockStatement();
        throwException.addStatement(ThrowRecordNotFoundStatement(domainClassNode, idExpression));

        return new IfStatement(
                notRecord,
                throwException,
                new BlockStatement()
        );
    }

    /*
        throw new RecordNotFoundException("Domain", "read", id)
     */
    static private ThrowStatement ThrowRecordNotFoundStatement(ClassNode domainClassNode, Expression idExpression) {

        ArgumentListExpression exceptionArguments = new ArgumentListExpression(
                new ConstantExpression(domainClassNode.getNameWithoutPackage()),
                new ConstantExpression("read"),
                idExpression
        );

        ConstructorCallExpression newRecordNotFoundException = new ConstructorCallExpression(
                ClassHelper.make("pr.exception.RecordNotFoundException"),
                exceptionArguments
        );

        return new ThrowStatement(newRecordNotFoundException);
    }

    /**
     * class DateRange implements Validateable {
     * <p>
     * // Properties
     * Date startDate = Date.valueOf(LocalDate.now())
     * Date endDate
     * <p>
     * static constraints = {
     * endDate:
     * NULLABLE
     * }
     * }
     *
     * @param sourceUnit
     */
    private void addDateRangeInnerClass(SourceUnit sourceUnit) {
        ClassNode implementsValidateable = DomainTransformation.createInterface("grails.validation.Validateable",
                (Modifier.ABSTRACT + Modifier.INTERFACE + Modifier.PUBLIC), null);
        InnerClassNode innerClassNode = DomainTransformation.createInnerClass(
                SUPER_CLASS,
                "pr.Account$EffectiveData",
                Modifier.STATIC, new ClassNode[]{implementsValidateable}, SUPER_CLASS);
        sourceUnit.getAST().addClass(innerClassNode);

        MethodCallExpression nowMethodCall = new MethodCallExpression(
                new ClassExpression(new ClassNode(java.time.LocalDate.class)),
                new ConstantExpression("now"),
                new ArgumentListExpression()
        );

        ArgumentListExpression arguments = new ArgumentListExpression();
        arguments.addExpression(nowMethodCall);
        MethodCallExpression valueOfMethodCall = new MethodCallExpression(
                new ClassExpression(new ClassNode("java.sql.Date", Modifier.PRIVATE, null)),
                new ConstantExpression("valueOf"),
                arguments
        );

        System.out.println("method " + valueOfMethodCall);

        ClassNode javaSqlDate = new ClassNode(java.sql.Date.class);
        javaSqlDate.setRedirect(new ClassNode(java.sql.Date.class));

        ClassNode validateError = new ClassNode(org.springframework.validation.Errors.class);
        validateError.setRedirect(new ClassNode(org.springframework.validation.Errors.class));

        //javaSqlDate.setRedirect(new ClassNode("java.sql.Date", Modifier.PRIVATE, null));

        FieldNode startDateFieldNode = new FieldNode(
                "startDate",
                Modifier.PRIVATE,
                javaSqlDate,
                innerClassNode,
                valueOfMethodCall
        );

        FieldNode endDateFieldNode = new FieldNode(
                "endDate",
                Modifier.PRIVATE,
                javaSqlDate,
                innerClassNode,
                new ConstantExpression(null)
        );



        MethodCallExpression valueOfError = new MethodCallExpression(
                new ClassExpression(new ClassNode("org.springframework.validation.Errors", Modifier.PUBLIC, null)),
                new ConstantExpression("valueOf"),
                arguments
        );

        FieldNode validateErrors = new FieldNode(
                "grails_validation_Validateable__errors",
                Modifier.PRIVATE,
                validateError,
                innerClassNode,
                null
        );


        innerClassNode.addProperty(new PropertyNode(startDateFieldNode, Modifier.PUBLIC, null, null));
        innerClassNode.addProperty(new PropertyNode(endDateFieldNode, Modifier.PUBLIC, null, null));
        //innerClassNode.addProperty(new PropertyNode(validateErrors, Modifier.PUBLIC, null, null));
        innerClassNode.addField(validateErrors);


    }

    private void displayInterfaces(ClassNode classNode) {
        System.out.println("\n****************************" + classNode.getName());
        classNode.getFields().stream().forEach(fieldNode ->
                {
                    System.out.println("fieldNode-name " + fieldNode.getName());
                    System.out.println("fieldNode-modifiers " + fieldNode.getModifiers());
                    System.out.println("fieldNode-text " + fieldNode.getText());
                    System.out.println("fieldNode-initialExp " + fieldNode.getInitialExpression());
                    System.out.println("fieldNode-initialValueEx " + fieldNode.getInitialValueExpression());
                    System.out.println("fieldNode-type " + fieldNode.getType());
                    System.out.println("fieldNode-type-name " + fieldNode.getType().getName());
                    System.out.println("fieldNode-type-super-class " + fieldNode.getType().getSuperClass());
                    System.out.println("fieldNode-type-modifiers " + fieldNode.getType().getModifiers());
                    System.out.println("fieldNode-type-isPrimaryClassNode " + fieldNode.getType().isPrimaryClassNode());
                }
        );
        classNode.getProperties().stream().forEach(propertyNode ->
                {
                    System.out.println("propertyNode " + propertyNode.getName());
                    System.out.println("propertyNode-modifiers " + propertyNode.getModifiers());
                    System.out.println("propertyNode-getter-block " + propertyNode.getGetterBlock());
                    System.out.println("propertyNode-setter-block " + propertyNode.getSetterBlock());
                }
        );
    }

    static ClassNode SUPER_CLASS = new ClassNode(java.lang.Object.class);

    static public InnerClassNode createInnerClass(ClassNode outerClass, String name, int modifiers, ClassNode[] interfaces, ClassNode superClass) {
        InnerClassNode innerClassNode = new InnerClassNode(
                outerClass,
                "pr.Account$DateRange",
                modifiers,
                superClass == null ? SUPER_CLASS : superClass,
                interfaces,
                null
        );
        return innerClassNode;
    }

    static public ClassNode createInterface(String name, int modifiers, ClassNode superClass) {
        return new ClassNode(
                name,
                modifiers,
                superClass == null ? SUPER_CLASS : superClass
        );

    }
}

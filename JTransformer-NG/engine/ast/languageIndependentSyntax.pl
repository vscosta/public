% Autor: Guenter Kniesel
% Date: 7.9.2004

 /*******************************************************************
  Predicates used to define a language's syntax.
    
  They describe the view of the language's program elements provided 
  to programmers writing conditional transformations (CTs).
    
  They are used to generate a language independent access layer to
  the internal representation of ASTs and to check syntactic correctness
  (of CTs and of the internal representation generated by a parser).

   ast_node_signature(?Language, ?Functor, ?Arity).
      The Abstract Syntax Tree (AST) for the language arg1 contains
      nodes with label (functor) arg2 and arity arg3.

   ast_node_term(Lang, Term)
      The Abstract Syntax Tree (AST) for the language arg1 contains
      nodes that unify with arg2. This predicate either checks its
      input or enumerates most general instances of all the legal
      AST node terms. 
 
   ast_node_def(?Language, ?AstNodeLabel, ?AstNodeArguments)
      The Abstract Syntax Tree (AST) for the language arg1 contains
      nodes with label (functor) arg2 and arguments conforming to the
      argument descriptions in arg3.

   ast_id(+Language, ?IdName)
      returns arg2 = <constant for id attribute>.
      
   ast_parent(+Language, ?ParentName)
      returns arg2 = <constant for parent attribute>.
      
   ast_sub_tree(?Language, ?ArgumentLabel)
      In all nodes of the AST of language arg1, the argument with
      name arg2 refers to a subtree of the respective AST node
      (provided that the node has an argument with that name).
     
   ast_node_type(?Language, ?Type)
      arg2 is a syntactic type in the AST of language arg1.

   ast_reference_type(?Language, ?Type)
      arg2 is a syntactic type in the syntax of language arg1,
      whose elements are identities of AST nodes.

   ast_node_subtype(?Language, ?SubType, ?SuperType)
      arg3 is a subtype of the syntactic type arg3 in language arg1.

  ***************************************************************** */


/**
 *  ast_node_term(Lang, Term)
 *
 *    The Abstract Syntax Tree (AST) for the language arg1 contains
 *    nodes that unify with arg2. This predicate either checks its
 *    input or enumerates most general instances of all the legal
 *    AST node terms. 
 */  
ast_node_term(Lang, Head) :-
    nonvar(Head),
    !,
    functor(Head,NodeType,Arity),
    ast_node_signature(Lang, NodeType, Arity).
ast_node_term(Lang, Head) :-
    var(Head),
    !,
    ast_node_signature(Lang, NodeType, Arity),
    functor(Head,NodeType,Arity).
  
    
/**
 *  ast_node_signature(?Language, ?NodeType, ?Arity)
 *
 *    The Abstract Syntax Tree (AST) for the language arg1 contains
 *    nodes with label (functor) arg2 and arity arg3.
 */
 
% This is the correct long term definition for the new Java syntax:
 
ast_node_signature_REAL(Lang, NodeType, Arity) :-
    ast_node_def(Lang, NodeType, ArgList),
    length(ArgList,Arity).
 
% Use the following hack during co-existence of old an new syntax version:
 
ast_node_signature(Lang, NodeType, Arity) :-
   ast_node_signature_REAL(Lang, NodeType, N),
   /* The hack in the following lines accounts for the integration
      of attributes as normal arguments in the new syntax. In a few
      cases it leads to longer parameter lists compared to the old
      syntax. These cases are 'corrected' by hand in the following.
      The real correction would be an adaptation of the PEF syntax.
   */
   (Lang = 'Java' 
    -> hackTreeSignature(NodeType,N,Arity)
     ; true).
  
hackTreeSignature(Functor,N,Arity) :- 
   ( (Functor = classDefT,  Arity = 4, !) 
   ; (Functor = methodDefT, Arity = 7, !)
   ; (Functor = fieldDefT,  Arity = 5, !)
   ; (Arity = N)
   ).   
       
 /**
  * ast_node_def(?Language, ?AstNodeLabel, ?AstNodeArguments)
  *
  * The Abstract Syntax Tree (AST) for the language arg1 contains 
  * nodes with label (functor) arg2 and arguments arg3. 
  *
  * Node arguments are described by a list of terms of the form
  *    ast_arg(ArgName, Cardinality, IdOrAttribute,  Types)
  * where 
  *    ArgName is the name of the argument (usually an atom)
  *    Cardinality is either 
  *        SCHEMA  | EXAMPLE | EXPLANATION
  *        --------+---------+------------------------------------
  *        *       | *       | Any cardinality, including 0.
  *        From-To | 0-1     | A cardinality range with the lower bound
  *                |         | From and the uper bound To (inclusive).
  *        Number  | 1       | Any positive integer denoting a fixed 
  *                |         | number of values (most often 1). 
  *       The cardinality 0 indicates that the value may be 'null'.
  *   IdOrAttribute is either
  *       id   Indicates that the value is the identity of an 
  *            AST node. By convention the first argument of any
  *            AST node is the id of that node and has ArgName 'id'.
  *            All other id arguments refer to other AST nodes. 
  *            We call them 'references'.
  *       attr Indicates that the value is not to be interpreted 
  *            as an id. It can be any legal Prolog term including
  *            compound terms.
  *   Types is a list of types defined by arg3 of ast_node_type/3. 
  *         Every value of the attribute must be from one of these
  *         types. The value 'null', legal if the cardinality includes
  *         0, is considered as an element of every types.
  *
  * Language independent bottom up traversals of an AST are supported 
  * by the convention that the second argument of every AST
  * node has the name 'parent' and refers to the parent node.
  *
  * Example (excerpt from Java AST definition):
  *
  * ast_node_def('Java', applyT,[
  *     ast_arg(id,      1, id,  [id]),              <-- convention!!!
  *     ast_arg(parent,  1, id,  [id]),              <-- convention!!!
  *     ast_arg(encl,    1, id,  [methodDefT,fieldDefT]),
  *     ast_arg(expr,  0-1, id,  [expressionType]),
  *     ast_arg(name,    1, attr,[atomType]),
  *     ast_arg(args,    1, id,  [expressionType]),
  *     ast_arg(ref,     1, id,  [methodDefT])
  * ]).
  *
  * This predicate must be defined for every language to be processed
  * by JTransformer. It is a multifile predicate. In case of 
  * differences, its autoritative documentation is the one in the file 
  * languageIndependent.pl.
  */
:- multifile ast_node_def/3.

  % No definition of ast_node_def here. Definitions are in each file
  % defining a language syntax (e.g. java) or a abstraction of a
  % language syntax (e.g. programming language, OO language, markup
  % language, ...).


   
/**
  * ast_id(+Language, ?IdName)
  *    returns arg2='id' for any arg1.
  *
  * ast_parent(+Language, ?ParentName)
  *    returns arg2='parent' for any arg1.
  *
  * These two definitions express the convention that in all languages
  * the attribute representing the id of a node is named "id" and
  * the attribute representing the parent of a node is named "parent".
  * The predicates makes sense in order not to hard-code these constants
  * in the rest of the system.
  */
ast_id(    _AnyLanguage, id).
ast_parent(_AnyLanguage, parent).  
 
 
 /**
  * ast_sub_tree(?Language, ?ArgumentLabel)
  *
  * In all nodes of the AST of language arg1, the argument with 
  * name arg2 refers to a subtree of the respective AST node 
  * (provided that the node has an argument with that name).
  *
  * This is the abstraction allowing language independent 
  * top down traversal of an AST. 
  * 
  * Language independent bottom up traversals are supported 
  * by the convention that the second argument of every AST
  * node has the name 'parent' and refers to the parent node.
  * See documentation of ast_node_def/3. 
  *
  * This is a multifile predicate. In case of differences, 
  * its autoritative documentation is the one in the file 
  * languageIndependent.pl.
  */ 
:- multifile ast_sub_tree/2.

  % No definition of ast_node_def here. Definitions are in each file
  % defining a language syntax (e.g. java) or a abstraction of a
  % language syntax (e.g. programming language, OO language, markup
  % language, ...).
 
 /* *************************************************************
    Syntactic typing of AST
    *********************************************************** */ 
  
 /** 
  * ast_node_type(?Language, ?Type)
  * 
  * arg2 is a syntactic type in the AST of language arg1.  
  * 
  * Legal types (values of arg2 or arg3) are
  *   - 'flag' = legal values just 'true' and 'false'
  *   - 'atom' =  legal values are atoms as defined in prolog
  *   - 'number' = legal values are number as defined in prolog
  *   - 'compound' = legal values are compound terms in prolog
  *   - 'nonRef' = supertype of 'atom', 'number' and 'compound'
  *   - Reference types as defined by ast_reference_type/2.
  *
  * This predicate may not be defined in other files.
  */ 

ast_node_type(_Lang,flag).
ast_node_type(_Lang,atom).
ast_node_type(_Lang,number).
ast_node_type(_Lang,compound). 
ast_node_type(_Lang,nonRef).
ast_node_type(_Lang,_Type) :-
    ast_reference_type(_Lang,_Type).

 /** 
  * ast_reference_type(?Language, ?Type)   
  * 
  * arg2 is a type whose elements are identities of AST nodes
  * in the syntax of language arg1.  
  * 
  * Legal reference types are:
  *   - 'id' = Values of type 'id' are identities of AST nodes as used 
  *            internally by JTransformer. Identities used to refer to 
  *            other nodes (foreign keys in a database lingo) are 
  *            called references and the types denoting them are
  *            the real reference types. They are defined as:
  *   - If 'label' is the label of an AST node of language L
  *     then it is also a legal AST reference type of L. It denotes
  *     all ids of nodes with label 'label' in L. 
  *
  * A language definition may define additional reference types
  * as abstractions (supertypes) of a group of AST nodes. 
  * See also ast_node_subtype/3.
  *
  * This is a multifile predicate. In case of differences, its 
  * autoritative documentation is the one in the file 
  * languageIndependent.pl.
  */ 
:- multifile ast_reference_type/2.

ast_reference_type(_Lang, id).     % supertype of all reference types
ast_reference_type(Lang,Label) :-  % ast node labels are ref types
        ast_node_def(Lang,Label,_).


/** 
  * ast_node_subtype(?Language, ?SubType, ?SuperType)   
  * 
  * arg3 is a subtype of the AST type arg3 in language arg1.  
  * 
  * In every language syntax 
  * - the type 'id' is the common supertype of reference types
  * - the type 'nonRef' is the common supertype of non reference types
  *   ('atom', 'number' and 'compound')
  *    
  * A language definition can provide further clauses that
  * define supertypes for a group of AST nodes (e.g. expressions).
  * See the definition of the Java AST for examples.
  *
  * This is a multifile predicate. In case of differences, its 
  * autoritative documentation is the one in the file 
  * languageIndependent.pl.
  */ 
:- multifile ast_node_subtype/3.

ast_node_subtype(_,flag,atom).
ast_node_subtype(_,atom,nonRef).
ast_node_subtype(_,number,nonRef). 
ast_node_subtype(_,compound,nonRef).
ast_node_subtype(_,T1,id) :- 
    ast_reference_type(_,T1).
ast_node_subtype(L,T1,T2) :- 
    ast_node_subtype(L,T1,T),
    ast_node_subtype(L,T,T2).
    

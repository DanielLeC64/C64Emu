
 � ***** HIRES-DEMO ***** $ � j V�13�4096 : VC�V�13�256 :               � POINTERS TO VIDEO CHIPS � � VC,�(VC)� 254 :               � SET UP VIC CHIP � � V�24,8 : S�24�1024 : C�16�1024 :   � CHANGE VIDEO MATRIX BASE 	( � V�17,�(V�17)� 32 :             � SELECT BIT MAP MODE %	, � k	- � THE HIRES SCREEN IS NOW AT $6000        AND THE COLOUR AT $4000 q	. � �	2 � X�0 � 8192 : � S�X,0 : � �	< � X�0 � 1024 : � C�X,1 : � �	@ � �	A � THAT CLEARED THE GRAPHICS AREA          AND SET IT ALL TO WHITE 
B � 
F W1�10 : W2�100 X
P � I�0 � 6.5 � 0.02 :              X�W1��(I)�160 : Y�W2��(I)�100 �
Z X��(X�0.5) : Y��(Y�0.5) :           � 2000 �
_ � : W1�W1�5 : W2�W2�5 : � 80 �
�� �
�� ***** PLOT SUBROUTINE ***** �
�� �
�Y1��(Y�8) : Y2�Y�Y1�8 
�X1��(X�8) : X2�X�X1�8 2�CH�(Y1�320)�(X1�8)�Y2 : BI�2�(7�X2) M�� S�CH,�(S�CH)� BI : �   
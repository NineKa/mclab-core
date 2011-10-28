function [result] = u25_auto(n)
  x0 = double([0.134364244112,0.847433736937;0.763774618977,0.255069025739]*n);
  x1 = uint64([0.237964627092,0.544229225296;0.369955166548,0.603920038596]*n);
  result = complex(x0,x1);
end

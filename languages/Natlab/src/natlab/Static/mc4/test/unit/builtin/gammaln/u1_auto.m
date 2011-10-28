function [result] = u1_auto(n)
  x0 = double([0.134364244112,0.847433736937,0.763774618977;0.255069025739,0.495435087092,0.449491064789;0.651592972723,0.788723351136,0.0938595867742]*n);
  result = gammaln(x0);
end

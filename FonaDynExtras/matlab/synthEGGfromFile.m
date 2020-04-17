function [egg, Qc, maxDegg, Ic, ampl] = synthEGGfromFile(filename, points, periods)
% Resynthesize the EGG waveshapes from a FonaDyn cluster data file.
%  Two .csv file formats are supported, pre-v1.4.3 and from v1.4.3 .
%  All outputs are arrays with nClusters elements.
%  - egg is the vector of the pulse wave resynthesized with points*periods
%  - Qci is the quotient of contact by integration
%  - maxDegg (Qdelta) is the normalized peak dEGG
%  - Ic is the index of contacting
%  - ampl is the peak-to-peak amplitude of the total waveform

% Initialize variables.
delimiter = ';';
fEGGname = filename;
% Open the file, read as one large array
cData = dlmread(filename, delimiter);

% Parse cData depending on the file format (old <= 1.4.2; new >= 1.4.3)
if (cData(1,3) == 0) 
    nClusters = cData(1, 1);
    nDeltas = cData(1, 2) / 3;
    nCounts = cData(2,1:(nDeltas-1));
    % Build three arrays, of delta-levels, cosines and sines
    cArray = cData(3:(nClusters+2),:);
else
    nClusters = size(cData,1);
    nDeltas   = (size(cData,2)-1) / 3;
    nCounts   = cData(:,1);
    cArray    = cData(:,2:end);
end

% Build three arrays, of delta-levels, cosines and sines
dLevels = [zeros([nClusters,1]) cArray(1:nClusters,1:(nDeltas-1))];
ix = [nDeltas 1:(nDeltas-1)];
dCosines = cArray(1:nClusters, ix+nDeltas);
dSines = cArray(1:nClusters, ix+(2*nDeltas));

% Compute arrays of amplitudes and phases for each 'harmonic'
%amps = power(10, dLevels/2);
phases = atan2(dSines, dCosines);
for n = 1:nClusters
   phases(n,2:nDeltas) = phases(n,2:nDeltas) + phases(n,1);
end

waves = zeros(nClusters, points*periods);
Qc = zeros(nClusters,1);
maxDegg = zeros(nClusters,1);
Ic = zeros(nClusters,1);

for n = 1:nClusters
   [eggN, tStruct] = synthEGGfromArrays(dLevels(n,:), phases(n,:), nDeltas, points, periods);
   waves(n,:) = eggN;
   Qc(n) = tStruct.qC;
   maxDegg(n) = tStruct.maxDegg;
   Ic(n) = tStruct.iC;
   ampl(n) = tStruct.p2pAmpl;
end

egg = waves';
end

package pl.edu.agh.emotionalrobot.recognizers.audio.utils;


public class LibrosaMFCC {

    // default args
    private Integer samplingRate = 44100;
    private Integer numberOfMfccsToReturn = 25; //not default but this is used by nn creators
    private Integer lengthOfFftWindow = 2048;

    private LibrosaFFT librosaFft = new LibrosaFFT();

    public float[] process(double[] yArray) {
        double[][] spectrogram = powerToDb(melspectrogram(yArray));
        float[] finalResult = compressResult(spectrogram);
        return finalResult;
    }

    private float[] compressResult(double[][] spectrogram) {
        double[][] dctBasis = filtersDct();
        double[][] mfccSpectrogram = new double[numberOfMfccsToReturn][spectrogram[0].length];
        for (int i = 0; i < numberOfMfccsToReturn; i++){
            for (int j = 0; j < spectrogram[0].length; j++){
                mfccSpectrogram[i][j] = 0.0;
                for (int k = 0; k < spectrogram.length; k++){
                    mfccSpectrogram[i][j] += dctBasis[i][k]*spectrogram[k][j];
                }
            }
        }
        float[] finalResult = new float[mfccSpectrogram.length * mfccSpectrogram[0].length];
        for (int i = 0; i < finalResult.length; i++) {
            finalResult[i] = (float) mfccSpectrogram[i % mfccSpectrogram.length][i / mfccSpectrogram.length];
        }
        return finalResult;
    }


    private double[][] powerToDb(double[][] spectrogram) {
//     Convert a power spectrogram (amplitude squared) to decibel (dB) units
//    This computes the scaling ``10 * log10(S / ref)`` in a numerically
//    stable way.
//        ref=1.0, amin=1e-10, top_db=80.0
//        amin - minimum threshold for abs(spectrogram)
//        top_db -  threshold the output at `top_db` below the peak:
//        ``max(10 * log10(S)) - top_db``
        double amin = 1e-10;
        double topDb = 80.0;
        double maxValue = -1000.0;
        double[][] logSpectogram = new double[spectrogram.length][spectrogram[0].length];
        for (int i = 0; i < spectrogram.length; i++) {
            for (int j = 0; j < spectrogram[0].length; j++) {
                double magnitude = Math.abs(spectrogram[i][j]);
                logSpectogram[i][j] = Math.log10((amin > magnitude) ? amin : magnitude);
                if (logSpectogram[i][j] > maxValue)
                    maxValue = logSpectogram[i][j];
            }
        }
        for (int i = 0; i < spectrogram.length; i++) {
            for (int j = 0; j < spectrogram[0].length; j++) {
                if (logSpectogram[i][j] < maxValue - topDb)
                    maxValue = maxValue - topDb;
            }
        }

        return logSpectogram;
    }

    private double[][] melspectrogram(double[] yArray) {
//S, n_fft = _spectrogram(y=y, S=S, n_fft=n_fft, hop_length=hop_length, power=power,
//                            win_length=win_length, window=window, center=center,
//                            pad_mode=pad_mode)
//    # Build a Mel filter
//    mel_basis = filters.mel(sr, n_fft, **kwargs)
        Integer numberOfSamplesBetweenSuccessiveFrames = 512;
        double[][] stftSpectrogram = stft(yArray, numberOfSamplesBetweenSuccessiveFrames);
        double[][] melBasis = filtersMel();
//    return np.dot(mel_basis, S)
        double[][] melSpectrogram = new double[melBasis.length][stftSpectrogram[0].length];
        for (int i = 0; i < melBasis.length; i++) {
            for (int j = 0; j < stftSpectrogram[0].length; j++) {
                for (int k = 0; k < melBasis[0].length; k++) {
                    melSpectrogram[i][j] += melBasis[i][k] * stftSpectrogram[k][j];
                }
            }
        }
        return melSpectrogram;
//

//     def _spectrogram(y=None, S=None, n_fft=2048, hop_length=512, power=1,
//                 win_length=None, window='hann', center=True, pad_mode='reflect'):
//    '''Helper function to retrieve a magnitude spectrogram.
//        # Otherwise, compute a magnitude spectrogram from input
//        S = np.abs(stft(y, n_fft=n_fft, hop_length=hop_length,
//                        win_length=win_length, center=center,
//                        window=window, pad_mode=pad_mode))**power
//
    }

    private double[][] stft(double[] yArray, Integer numberOfSamplesBetweenSuccesiveFrames) {
//     """Short-time Fourier transform (STFT). [1]_ (chapter 2)
//
//    The STFT represents a signal in the time-frequency domain by
//    computing discrete Fourier transforms (DFT) over short overlapping
//    windows.


//        # By default, use the entire frame
//    if win_length is None:
//        win_length = n_fft
        int windowLength = lengthOfFftWindow;
//    # Set the default hop, if it's not already specified
//    if hop_length is None:
//        hop_length = int(win_length // 4)
        int hopLength = numberOfSamplesBetweenSuccesiveFrames;
//    fft_window = get_window(window, win_length, fftbins=True)
        double[] fftWindow = getHannWindow(windowLength);
//    # Pad the window out to n_fft size
//    fft_window = util.pad_center(fft_window, n_fft)
//    # Reshape so that the window can be broadcast
//    fft_window = fft_window.reshape((-1, 1))
//    # Check audio is valid
//    util.valid_audio(y)
//    # Pad the time series so that frames are centered
//    if center:
//        y = np.pad(y, int(n_fft // 2), mode=pad_mode)
        double[] y = padY(yArray);
//
//    # Window the time series.
//    y_frames = util.frame(y, frame_length=n_fft, hop_length=hop_length)
        double[][] yFrames = makeFrames(y, windowLength, hopLength);
//    # Pre-allocate the STFT matrix
//    stft_matrix = np.empty((int(1 + n_fft // 2), y_frames.shape[1]),
//                           dtype=dtype,
//                           order='F')
        double[][] stftMatrix = new double[(int) (1 + windowLength / 2)][yFrames[0].length];
//    librosaFft = get_fftlib()
        double[] columns = new double[windowLength];
//    # how many columns can we fit within MAX_MEM_BLOCK?
//    n_columns = int(util.MAX_MEM_BLOCK / (stft_matrix.shape[0] *
//                                          stft_matrix.itemsize))
        for (int i = 0; i < stftMatrix[0].length; i++) {
            for (int j = 0; j < windowLength; j++) {
                columns[j] = fftWindow[j] * yFrames[j][i];
            }
            double[] spec = fftSpec(columns);
            for (int j = 0; j < 1 + windowLength / 2; j++) {
                stftMatrix[j][i] = spec[j];
            }
        }
//    for bl_s in range(0, stft_matrix.shape[1], n_columns):
//        bl_t = min(bl_s + n_columns, stft_matrix.shape[1])
//
//        stft_matrix[:, bl_s:bl_t] = librosaFft.rfft(fft_window *
//                                             y_frames[:, bl_s:bl_t],
//                                             axis=0)
//    return stft_matrix
        return stftMatrix;
    }

    private double[] fftSpec(double[] frame) {
        librosaFft.process(frame);
        double[] result = new double[frame.length];
        for (int i = 0; i < frame.length; i++) {
            result[i] = librosaFft.real[i] * librosaFft.real[i] + librosaFft.imag[i] * librosaFft.imag[i];
        }
        return result;
    }

    private double[][] makeFrames(double[] y, int windowLength, int hopLength) {
        int n_frames = 1 + (y.length - windowLength) / hopLength;
        double[][] frames = new double[windowLength][n_frames];
        for (int i = 0; i < windowLength; i++) {
            for (int j = 0; j < n_frames; j++) {
                frames[i][j] = y[j * hopLength + i];
            }
        }
        return frames;
    }

    private double[] padY(double[] yArray) {
        double[] ypad = new double[lengthOfFftWindow + yArray.length];
        for (int i = 0; i < lengthOfFftWindow / 2; i++) {
            ypad[(lengthOfFftWindow / 2) - i - 1] = yArray[i + 1];
            ypad[(lengthOfFftWindow / 2) + yArray.length + i] = yArray[yArray.length - 2 - i];
        }
        for (int j = 0; j < yArray.length; j++) {
            ypad[(lengthOfFftWindow / 2) + j] = yArray[j];
        }
        return ypad;
    }

    private double[] getHannWindow(int windowLength) {
//        general_hamming(M, 0.5, sym=True)
//        general_cosine(M, [alpha, 1. - alpha], sym)
        double[] window = new double[windowLength];
        double step = 2.0 * Math.PI / windowLength;
        for (int i = 0; i < windowLength; i++) {
            window[i] = 0.5 - Math.cos(i * step);
        }
        return window;
    }

    private double[][] filtersMel() {
//        Create a Filterbank matrix to combine LibrosaFFT bins into Mel-frequency bins
        int numberOfMelBandsToGenerate = 128;
        int lengthOfTheFftWindow = 2048;
//        # Initialize the weights
        double[][] weights = new double[numberOfMelBandsToGenerate][1 + lengthOfTheFftWindow / 2];
//      # Center freqs of each LibrosaFFT bin
        double[] fftFreqs = fftFrequencies();
//    fftfreqs = fft_frequencies(sr=sr, n_fft=n_fft)
//
//    # 'Center freqs' of mel bands - uniformly spaced between limits
//    mel_f = mel_frequencies(n_mels + 2, fmin=fmin, fmax=fmax, htk=htk)
        double[] melFreqs = melFrequencies(numberOfMelBandsToGenerate + 2);
//    fdiff = np.diff(mel_f)
        double[] fdiff = new double[melFreqs.length - 1];
        for (int i = 0; i < melFreqs.length - 1; i++) {
            fdiff[i] = melFreqs[i + 1] - melFreqs[i];
        }
//    ramps = np.subtract.outer(mel_f, fftfreqs)
        double[][] ramps = new double[melFreqs.length][fftFreqs.length];
        for (int i = 0; i < melFreqs.length; i++) {
            for (int j = 0; j < fftFreqs.length; j++) {
                ramps[i][j] = melFreqs[i] - fftFreqs[j];
            }
        }
//    for i in range(n_mels):
//        # lower and upper slopes for all bins
//        lower = -ramps[i] / fdiff[i]
//        upper = ramps[i+2] / fdiff[i+1]
//
//        # .. then intersect them with each other and zero
//        weights[i] = np.maximum(0, np.minimum(lower, upper))
        for (int i = 0; i < numberOfMelBandsToGenerate; i++) {
            for (int j = 0; j < ramps[i].length; j++) {
                double lowerSlope = -ramps[i][j] / fdiff[i];
                double upperSlope = ramps[i + 2][j] / fdiff[i + 1];
                double minimum = Math.min(lowerSlope, upperSlope);
                weights[i][j] = Math.max(minimum, 0.0);
            }

        }
//    if norm == 1:
//        # Slaney-style mel is scaled to be approx constant energy per channel
//        enorm = 2.0 / (mel_f[2:n_mels+2] - mel_f[:n_mels])
//        weights *= enorm[:, np.newaxis]
        for (int i = 0; i < numberOfMelBandsToGenerate; i++) {
            double enorm = 2.0 / (melFreqs[i + 2] - melFreqs[i]);
            for (int j = 0; j < weights[i].length; j++) {
                weights[i][j] *= enorm;
            }
        }
//
//    # Only check weights if f_mel[0] is positive
//    if not np.all((mel_f[:-2] == 0) | (weights.max(axis=1) > 0)):
//        # This means we have an empty channel somewhere
//        warnings.warn('Empty filters detected in mel frequency basis. '
//                      'Some channels will produce empty responses. '
//                      'Try increasing your sampling rate (and fmax) or '
//                      'reducing n_mels.')
        return weights;
    }


    private double[][] filtersDct() {
        // numberOfMfccsToReturn
        int numberOfMelBandsToGenerate = 128;
        double[][] weights = new double[numberOfMfccsToReturn][numberOfMelBandsToGenerate];
        for (int j = 0; j < numberOfMelBandsToGenerate; j++) {
            weights[0][j] = 1.0 / Math.sqrt(numberOfMelBandsToGenerate);
        }

        for (int i = 1; i < numberOfMfccsToReturn; i++) {
            for (int j = 0; j < numberOfMelBandsToGenerate; j++) {
                weights[i][j] = Math.cos(i * (1 + 2 * j) * Math.PI / (2.0 * numberOfMelBandsToGenerate)) * Math.sqrt(2.0/numberOfMelBandsToGenerate);
            }
        }
        return weights;
    }

    private double[] melFrequencies(int nMels) {
//        mel_frequencies(n_mels=128, fmin=0.0, fmax=11025.0, htk=False):
//    Compute an array of acoustic frequencies tuned to the mel scale.
//        # 'Center freqs' of mel bands - uniformly spaced between limits
//    min_mel = hz_to_mel(fmin, htk=htk) 2595.0 * np.log10(1.0 + frequencies / 700.0)
//    max_mel = hz_to_mel(fmax, htk=htk)
//
//    mels = np.linspace(min_mel, max_mel, n_mels)
//
//    return mel_to_hz(mels, htk=htk) 700.0 * (10.0**(mels / 2595.0) - 1.0)
        double minMel = hzToMel(0.0);
        double maxMel = hzToMel((double) samplingRate / 2);
        double step = (maxMel - minMel) / (nMels - 1);
        double[] freqs = new double[nMels];
        for (int i = 0; i < nMels; i++) {
            freqs[i] = melToHz(minMel + (step * i));
        }
        return freqs;
    }

    private double hzToMel(double v) {
        return 2595.0 * Math.log10(1.0 + v / 700.0);
    }

    private double melToHz(double v) {
        return 700.0 * (Math.pow(10, v / 2595.0) - 1.0);
    }

    private double[] fftFrequencies() {
//        start, stop, num=50, endpoint=True, retstep=False, dtype=None, axis=0)
//        start = 0
//        stop = float(samplingRate) /2
//        num = int( 1 + n_fft // 2)
        int num = (int) (1 + lengthOfFftWindow / 2);
        double start = 0.0;
        double stop = (double) samplingRate / 2;
        double step = (stop - start) / (num - 1);
        double[] freqs = new double[num];
        for (int i = 0; i < num; i++) {
            freqs[i] = start + i * step;
        }
        return freqs;
    }
}
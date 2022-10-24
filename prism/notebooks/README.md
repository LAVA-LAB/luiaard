

# plotting final results


## installing prerequisites

- ensure `pipenv` is installed. Visit [this page](https://github.com/pypa/pipenv#installation) for installation instructions.

- install all the libraries in a virtual environment:

```
pipenv install
```

## running the scripts

- launch the virtual environment created:
```
pipenv shell
```

- start the jupyter notebook server:
```
sh make_basic_plots.sh
sh make_strength_plots.sh
```

- the plots are stored on the directory `../results`

## customizing the plots

The file `paper.mplstyle` defines some customizations for the plots
See [this page](https://matplotlib.org/stable/tutorials/introductory/customizing.html) link for more options
